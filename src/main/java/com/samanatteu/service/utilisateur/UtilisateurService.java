package com.samanatteu.service.utilisateur;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.utilisateur.CreationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.ModificationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.entity.abonnement.Formule;
import com.samanatteu.exception.abonnement.FormuleIntrouvableException;
import com.samanatteu.exception.abonnement.FormuleNonDisponibleException;
import com.samanatteu.exception.abonnement.FormuleObligatoireException;
import com.samanatteu.exception.abonnement.FormuleSurDevisException;
import com.samanatteu.exception.onboarding.InvitationRequiseException;
import com.samanatteu.repository.abonnement.FormuleRepository;
import com.samanatteu.service.auth.VerificationEmailService;
import com.samanatteu.service.onboarding.InvitationService;
import com.samanatteu.exception.utilisateur.EmailDejaUtiliseException;
import com.samanatteu.exception.utilisateur.EmailObligatoireException;
import com.samanatteu.exception.utilisateur.TelephoneDejaUtiliseException;
import com.samanatteu.repository.tontine.NbMembresParGestionnaire;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class UtilisateurService {

    private final PasswordEncoder passwordEncoder;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final TelephoneService telephoneService;
    private final ParticipationRepository participationRepository;
    private final InvitationService invitationService;
    private final FormuleRepository formuleRepository;
    private final VerificationEmailService verificationEmailService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
            UtilisateurConnecte utilisateurConnecte, TelephoneService telephoneService,
            ParticipationRepository participationRepository, InvitationService invitationService,
            FormuleRepository formuleRepository, VerificationEmailService verificationEmailService) {
        this.invitationService = invitationService;
        this.formuleRepository = formuleRepository;
        this.verificationEmailService = verificationEmailService;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.utilisateurConnecte = utilisateurConnecte;
        this.telephoneService = telephoneService;
        this.participationRepository = participationRepository;
    }

    // La liste de l'ADMIN : les GESTIONNAIRE et les ADMIN seulement. Les membres sont les
    // clients des gestionnaires, pas de la plateforme : aucun nom, téléphone ni email de
    // membre ne sort d'ici (frontière SaaS). D'un gestionnaire, on donne juste son nombre
    // de membres distincts (hors SORTI).
    public List<UtilisateurDTO> listUtilisateurs() {
        // Une requête pour tous les gestionnaires, rangée en Map gestionnaireId -> nombre.
        Map<Long, Long> nbMembres = participationRepository
                .compterMembresParGestionnaire(StatutParticipation.SORTI)
                .stream()
                .collect(Collectors.toMap(NbMembresParGestionnaire::gestionnaireId,
                        NbMembresParGestionnaire::nbMembres));

        return utilisateurRepository.findByRoleNot(RoleUtilisateur.MEMBRE)
                .stream()
                .map(utilisateur -> {
                    UtilisateurDTO dto = convertiUtilisateurDTO(utilisateur);
                    if (utilisateur.getRole() == RoleUtilisateur.GESTIONNAIRE) {
                        // Pas de ligne dans la Map = aucun membre inscrit.
                        dto.setNbMembres(nbMembres.getOrDefault(utilisateur.getId(), 0L));
                    }
                    return dto;
                })
                .toList();
    }

    // @Transactional : le compte et son lien de vérification sont écrits ensemble
    // (pas de gestionnaire non vérifiée sans lien pour se vérifier).
    @Transactional
    public UtilisateurDTO createUtilisateur(CreationUtilisateurDTO dto) {
        // Ordre des contrôles : le DROIT d'abord, la faisabilité ensuite (même règle que
        // ParticipationService). Un anonyme qui demande un ADMIN reçoit 403, même si son
        // numéro est mal écrit : le 400 viendrait après, et seulement s'il a le droit.
        // Seul un ADMIN peut créer un autre ADMIN (la route d'inscription est publique).
        if (dto.getRole() == RoleUtilisateur.ADMIN && !utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN)) {
            throw new AccesRefuseException();
        }
        // Un ADMIN ne crée pas de MEMBRE : un membre s'inscrit lui-même (route publique)
        // ou est ajouté par son gestionnaire. La plateforme ne gère pas les membres.
        if (dto.getRole() == RoleUtilisateur.MEMBRE && utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN)) {
            throw new AccesRefuseException();
        }
        // Le numéro saisi est normalisé en E.164 : "775550000" et "+221775550000" sont
        // le même compte, pas deux.
        String telephone = telephoneService.normaliser(dto.getTelephone());
        // Un visiteur ne devient MEMBRE que par un lien d'invitation valide (et,
        // pour une invitation nominative, avec le numéro invité : d'où le contrôle
        // APRÈS la normalisation). Une gestionnaire connectée ajoute ses membres
        // sans lien.
        if (dto.getRole() == RoleUtilisateur.MEMBRE && !utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE)) {
            if (dto.getJetonInvitation() == null || dto.getJetonInvitation().isBlank()) {
                throw new InvitationRequiseException();
            }
            invitationService.verifierPourInscription(dto.getJetonInvitation(), telephone);
        }
        // L'email est OBLIGATOIRE pour les gestionnaires (elles paient : on doit
        // pouvoir les facturer) et pour les administrateurs. Les membres l'ont
        // toujours facultatif — d'où un contrôle dépendant du rôle, que Bean
        // Validation ne peut pas exprimer sur un champ.
        boolean doitAvoirEmail = dto.getRole() == RoleUtilisateur.ADMIN
                || dto.getRole() == RoleUtilisateur.GESTIONNAIRE;
        if (doitAvoirEmail && (dto.getEmail() == null || dto.getEmail().isBlank())) {
            throw new EmailObligatoireException(dto.getRole().name());
        }
        // L'email est optionnel pour les membres : son unicité n'est vérifiée
        // que s'il est fourni (sinon existsByEmail(null) bloquerait tous les
        // comptes sans email après le premier).
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && utilisateurRepository.existsByEmail(dto.getEmail())) {
            throw new EmailDejaUtiliseException(dto.getEmail());
        }
        if (utilisateurRepository.existsByTelephone(telephone)) {
            throw new TelephoneDejaUtiliseException(telephone);
        }
        // Une gestionnaire qui s'inscrit SEULE (site public) choisit sa formule et
        // doit vérifier son email avant de se connecter. Créée par l'ADMIN, elle
        // est vérifiée d'office (l'ADMIN active son abonnement à part).
        boolean inscriptionPublique = dto.getRole() == RoleUtilisateur.GESTIONNAIRE
                && !utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN);
        Formule formule = inscriptionPublique ? formuleChoisie(dto.getFormuleId()) : null;

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setTelephone(telephone);
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setRole(dto.getRole());
        utilisateur.setEmailVerifie(!inscriptionPublique);

        Utilisateur enregistre = utilisateurRepository.save(utilisateur);
        if (inscriptionPublique) {
            verificationEmailService.envoyerLien(enregistre, formule);
        }
        return convertiUtilisateurDTO(enregistre);
    }

    // La formule d'un essai : elle doit exister, être en vente, et avoir un prix
    // (0 = « sur devis » : on passe par la page Contact).
    private Formule formuleChoisie(Long formuleId) {
        if (formuleId == null) {
            throw new FormuleObligatoireException();
        }
        Formule formule = formuleRepository.findById(formuleId)
                .orElseThrow(() -> new FormuleIntrouvableException(formuleId));
        if (!formule.getActive()) {
            throw new FormuleNonDisponibleException(formule.getNom());
        }
        if (formule.getPrixMensuel() == null || formule.getPrixMensuel().signum() == 0) {
            throw new FormuleSurDevisException(formule.getNom());
        }
        return formule;
    }

    public Optional<UtilisateurDTO> updateUtilisateur(Long id, ModificationUtilisateurDTO modifications) {
        return utilisateurRepository.findById(id).map(utilisateur -> {
            // Règle "propre profil" : seul le propriétaire du compte ou un ADMIN peut le modifier.
            if (!utilisateurConnecte.telephone().equals(utilisateur.getTelephone()) &&
                    !utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN)) {
                throw new AccesRefuseException();
            }
            // Le compte d'un MEMBRE ne regarde que lui (et son gestionnaire) : l'ADMIN
            // n'y touche pas, même s'il connaît son id.
            if (utilisateur.getRole() == RoleUtilisateur.MEMBRE && utilisateurConnecte.aLeRole(RoleUtilisateur.ADMIN)) {
                throw new AccesRefuseException();
            }

            // En base, "pas d'email" s'écrit toujours null, jamais "" : sinon deux comptes
            // sans email entreraient en conflit sur la contrainte d'unicité.
            String nouvelEmail = (modifications.getEmail() == null || modifications.getEmail().isBlank())
                    ? null
                    : modifications.getEmail();
            if (nouvelEmail != null
                    && !nouvelEmail.equals(utilisateur.getEmail())
                    && utilisateurRepository.existsByEmail(nouvelEmail)) {
                throw new EmailDejaUtiliseException(nouvelEmail);
            }

            utilisateur.setNom(modifications.getNom());
            utilisateur.setPrenom(modifications.getPrenom());
            utilisateur.setEmail(nouvelEmail);
            return convertiUtilisateurDTO(utilisateurRepository.save(utilisateur));
        });
    }

    // Route réservée à l'ADMIN (SecurityConfig) : il supprime des gestionnaires ou des
    // admins, jamais un MEMBRE (403). false = compte inexistant (404).
    public boolean deleteUtilisateur(Long id) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(id);
        if (utilisateur.isEmpty()) {
            return false;
        }
        if (utilisateur.get().getRole() == RoleUtilisateur.MEMBRE) {
            throw new AccesRefuseException();
        }
        utilisateurRepository.deleteById(id);
        return true;
    }

    private UtilisateurDTO convertiUtilisateurDTO(Utilisateur utilisateur) {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setEmail(utilisateur.getEmail());
        dto.setRole(utilisateur.getRole());
        dto.setActif(utilisateur.getActif());
        dto.setCreatedAt(utilisateur.getCreatedAt());
        dto.setUpdatedAt(utilisateur.getUpdatedAt());
        return dto;
    }
}
