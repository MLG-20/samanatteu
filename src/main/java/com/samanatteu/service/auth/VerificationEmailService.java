package com.samanatteu.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.auth.RenvoiVerificationDTO;
import com.samanatteu.entity.abonnement.Formule;
import com.samanatteu.entity.auth.VerificationEmail;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.exception.auth.LienVerificationInvalideException;
import com.samanatteu.exception.utilisateur.TelephoneInvalideException;
import com.samanatteu.repository.auth.VerificationEmailRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.service.abonnement.AbonnementService;
import com.samanatteu.service.notification.EnvoyeurEmail;
import com.samanatteu.service.utilisateur.TelephoneService;

// La vérification de l'email d'une gestionnaire inscrite par le site public :
// un lien à usage unique, valable 24 h, envoyé par email.
@Service
public class VerificationEmailService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailService.class);

    static final int VALIDITE_HEURES = 24;
    // Un renvoi au plus par minute et par compte : « Renvoyer l'email » ne doit
    // pas servir à inonder une boîte mail.
    static final int DELAI_RENVOI_SECONDES = 60;

    private final VerificationEmailRepository verificationEmailRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AbonnementService abonnementService;
    private final TelephoneService telephoneService;
    private final EnvoyeurEmail envoyeurEmail;
    private final String frontUrl;
    // SecureRandom (imprévisible), comme pour les liens d'invitation.
    private final SecureRandom hasard = new SecureRandom();

    public VerificationEmailService(VerificationEmailRepository verificationEmailRepository,
            UtilisateurRepository utilisateurRepository, AbonnementService abonnementService,
            TelephoneService telephoneService, EnvoyeurEmail envoyeurEmail,
            @Value("${samanatteu.front-url}") String frontUrl) {
        this.verificationEmailRepository = verificationEmailRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.abonnementService = abonnementService;
        this.telephoneService = telephoneService;
        this.envoyeurEmail = envoyeurEmail;
        this.frontUrl = frontUrl;
    }

    // Crée un lien pour ce compte (la formule choisie voyage avec lui) et l'envoie.
    // Seule l'EMPREINTE du jeton est enregistrée : le jeton n'existe que dans l'email.
    public void envoyerLien(Utilisateur gestionnaire, Formule formule) {
        String jeton = genererJeton();

        VerificationEmail verification = new VerificationEmail();
        verification.setUtilisateur(gestionnaire);
        verification.setFormule(formule);
        verification.setJetonHache(empreinte(jeton));
        verification.setExpireAt(LocalDateTime.now().plusHours(VALIDITE_HEURES));
        verification.setUtilise(false);
        // Lu en mémoire par les tests mockés (@PrePersist ne tourne qu'au vrai flush JPA).
        verification.setCreatedAt(LocalDateTime.now());
        verificationEmailRepository.save(verification);

        // Un email raté ne doit pas annuler l'inscription : le compte existe, et
        // la gestionnaire pourra redemander le lien (« Renvoyer l'email »).
        try {
            envoyeurEmail.envoyerAvecLien(gestionnaire.getEmail(), "Confirmez votre adresse email",
                    "Bonjour " + gestionnaire.getPrenom() + ", merci pour votre inscription sur SamaNatteu ! "
                            + "Confirmez votre adresse pour démarrer vos " + AbonnementService.DUREE_ESSAI_JOURS + " jours d'essai gratuit de la formule "
                            + formule.getNom() + ". Ce lien est valable " + VALIDITE_HEURES + " heures.",
                    "Confirmer mon adresse", frontUrl + "/verifier-email/" + jeton);
        } catch (RuntimeException e) {
            logger.warn("Échec de l'envoi de l'email de vérification à l'utilisateur {}", gestionnaire.getId(), e);
        }
    }

    // Le lien est cliqué : email vérifié + essai démarré sur la formule du lien.
    // @Transactional : les trois écritures (lien consommé, compte vérifié, essai)
    // passent ensemble ou pas du tout. Ici rien à garder en cas d'échec (pas de
    // compteur d'essais comme pour le code SMS), donc tout annuler est le bon choix.
    @Transactional
    public void verifier(String jeton) {
        VerificationEmail verification = verificationEmailRepository.findByJetonHache(empreinte(jeton))
                .orElseThrow(() -> new LienVerificationInvalideException());
        Utilisateur gestionnaire = verification.getUtilisateur();

        // Compte déjà vérifié (un autre lien a servi) : refusé aussi, sinon chaque
        // ancien lien redémarrerait 30 jours d'essai.
        if (verification.isUtilise()
                || verification.getExpireAt().isBefore(LocalDateTime.now())
                || gestionnaire.isEmailVerifie()) {
            throw new LienVerificationInvalideException();
        }

        verification.setUtilise(true);
        verificationEmailRepository.save(verification);

        gestionnaire.setEmailVerifie(true);
        utilisateurRepository.save(gestionnaire);

        abonnementService.demarrerEssai(gestionnaire, verification.getFormule());
    }

    // « Renvoyer l'email » : un nouveau lien, sur la formule choisie à l'inscription.
    // Toujours silencieux (la route répond 204 dans tous les cas) : compte inconnu,
    // déjà vérifié ou demande trop rapprochée ne se distinguent pas d'un envoi réussi.
    public void renvoyer(RenvoiVerificationDTO dto) {
        Optional<Utilisateur> existant = trouver(dto);
        if (existant.isEmpty() || existant.get().isEmailVerifie()) {
            return;
        }
        Utilisateur gestionnaire = existant.get();

        // Un compte non vérifié a toujours au moins un lien (écrit avec lui,
        // voir UtilisateurService.createUtilisateur) : c'est là qu'est sa formule.
        Optional<VerificationEmail> dernier = verificationEmailRepository
                .findFirstByUtilisateurIdOrderByCreatedAtDesc(gestionnaire.getId());
        if (dernier.isEmpty()
                || dernier.get().getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(DELAI_RENVOI_SECONDES))) {
            return;
        }
        envoyerLien(gestionnaire, dernier.get().getFormule());
    }

    // Même recherche qu'à la connexion (AuthService.login). Un numéro mal formé
    // est traité comme un compte inconnu : rien ne doit filtrer.
    private Optional<Utilisateur> trouver(RenvoiVerificationDTO dto) {
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            return utilisateurRepository.findByEmail(dto.getEmail());
        }
        try {
            return utilisateurRepository.findByTelephone(telephoneService.normaliser(dto.getTelephone()));
        } catch (TelephoneInvalideException e) {
            return Optional.empty();
        }
    }

    // 32 octets aléatoires (256 bits) en Base64 URL : impossible à deviner, et
    // utilisable tel quel dans un lien. Même recette que les invitations.
    private String genererJeton() {
        byte[] octets = new byte[32];
        hasard.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    // SHA-256 en hexadécimal (64 caractères). Toujours la même empreinte pour le
    // même jeton : on retrouve la ligne à partir du jeton reçu (pas BCrypt, dont
    // le sel aléatoire change l'empreinte à chaque fois).
    static String empreinte(String jeton) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti par toute JVM : n'arrive jamais.
            throw new IllegalStateException(e);
        }
    }
}
