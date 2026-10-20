package com.samanatteu.service.auth;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.auth.MotDePasseOublieDTO;
import com.samanatteu.dto.auth.ReinitialisationMotDePasseDTO;
import com.samanatteu.entity.auth.CodeReinitialisation;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.exception.auth.CodeInvalideException;
import com.samanatteu.exception.utilisateur.TelephoneInvalideException;
import com.samanatteu.repository.auth.CodeReinitialisationRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.service.notification.EnvoyeurSms;
import com.samanatteu.service.utilisateur.TelephoneService;

@Service
public class MotDePasseOublieService {
    private final UtilisateurRepository utilisateurRepository;
    private final CodeReinitialisationRepository codeReinitialisationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnvoyeurSms envoyeurSms;
    private final TelephoneService telephoneService;
    private final SecureRandom hasard = new SecureRandom();
    private static final int TENTATIVES_MAX = 5;

    public MotDePasseOublieService(UtilisateurRepository utilisateurRepository,
            CodeReinitialisationRepository codeReinitialisationRepository, PasswordEncoder passwordEncoder,
            EnvoyeurSms envoyeurSms, TelephoneService telephoneService) {
        this.utilisateurRepository = utilisateurRepository;
        this.codeReinitialisationRepository = codeReinitialisationRepository;
        this.passwordEncoder = passwordEncoder;
        this.envoyeurSms = envoyeurSms;
        this.telephoneService = telephoneService;
    }

    public void demanderCode(MotDePasseOublieDTO dto) {
        // Numéro inconnu : arrêt en silence, sans erreur. Répondre « inconnu »
        // permettrait de découvrir quels numéros ont un compte.
        // Un numéro MAL FORMÉ est traité comme un numéro inconnu : cette route
        // répond toujours 204, elle ne révèle rien (pas même un format invalide).
        Optional<Utilisateur> existant;
        try {
            existant = utilisateurRepository.findByTelephone(telephoneService.normaliser(dto.getTelephone()));
        } catch (TelephoneInvalideException e) {
            return;
        }
        if (existant.isEmpty()) {
            return;
        }
        Utilisateur utilisateur = existant.get();

        String code = genererCode();

        // En base : seulement le hachage du code, et sa date limite (10 minutes).
        CodeReinitialisation codeReinitialisation = new CodeReinitialisation();
        codeReinitialisation.setUtilisateur(utilisateur);
        codeReinitialisation.setCodeHache(passwordEncoder.encode(code));
        codeReinitialisation.setExpireAt(LocalDateTime.now().plusMinutes(10));
        // Lu en mémoire par les tests mockés (@PrePersist ne tourne qu'au vrai flush JPA).
        codeReinitialisation.setCreatedAt(LocalDateTime.now());
        codeReinitialisationRepository.save(codeReinitialisation);

        // EnvoyeurSms et non NotificationService : celui-ci enregistre chaque message
        // dans la table notification, le code y serait lisible en clair.
        envoyeurSms.envoyer(utilisateur.getTelephone(),
                "SamaNatteu : votre code est " + code
                        + ". Il est valable 10 minutes. Ne le communiquez à personne.");

    }

    // Tous les refus lancent la même exception : la réponse ne dit jamais lequel.
    public void reinitialiser(ReinitialisationMotDePasseDTO dto) {
        // Un numéro mal formé est traité comme un code faux : même exception,
        // même réponse. Rien ne distingue « numéro invalide » de « code erroné ».
        Utilisateur utilisateur;
        try {
            utilisateur = utilisateurRepository.findByTelephone(telephoneService.normaliser(dto.getTelephone()))
                    .orElseThrow(() -> new CodeInvalideException());
        } catch (TelephoneInvalideException e) {
            throw new CodeInvalideException();
        }
        // Seul le dernier code demandé compte (celui du SMS le plus récent).
        CodeReinitialisation codeReinitialisation = codeReinitialisationRepository
                .findFirstByUtilisateurIdOrderByCreatedAtDesc(utilisateur.getId())
                .orElseThrow(() -> new CodeInvalideException());

        // Le code enregistré peut-il encore servir ? Vérifié avant de regarder le code tapé.
        if (codeReinitialisation.isUtilise()
                || codeReinitialisation.getExpireAt().isBefore(LocalDateTime.now())
                || codeReinitialisation.getTentatives() >= TENTATIVES_MAX) {
            throw new CodeInvalideException();
        }

        // Code faux : l'essai est compté ET enregistré avant le refus. Pas de
        // @Transactional sur cette méthode : l'exception annulerait ce comptage.
        if (!passwordEncoder.matches(dto.getCode(), codeReinitialisation.getCodeHache())) {
            codeReinitialisation.setTentatives(codeReinitialisation.getTentatives() + 1);
            codeReinitialisationRepository.save(codeReinitialisation);
            throw new CodeInvalideException();
        }

        // Code consommé AVANT de changer le mot de passe : en cas de panne entre les
        // deux, il reste un code perdu (sans danger) et non un code réutilisable.
        codeReinitialisation.setUtilise(true);
        codeReinitialisationRepository.save(codeReinitialisation);

        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getNouveauMotDePasse()));
        // Nouveau mot de passe = sessions ouvertes fermées (voir AuthService.deconnecter).
        utilisateur.setVersionSessions(utilisateur.getVersionSessions() + 1);

        utilisateurRepository.save(utilisateur);

    }

    // SecureRandom et pas Random : un code prévisible ne prouve rien. %06d complète
    // par des zéros à gauche (4821 → 004821) ; String, car un int perdrait ces zéros.
    private String genererCode() {
        return String.format("%06d", hasard.nextInt(1_000_000));
    }

}
