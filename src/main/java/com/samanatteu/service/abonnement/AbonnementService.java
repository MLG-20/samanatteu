package com.samanatteu.service.abonnement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.abonnement.AbonnementDTO;
import com.samanatteu.dto.abonnement.ActivationAbonnementDTO;
import com.samanatteu.entity.abonnement.Abonnement;
import com.samanatteu.entity.abonnement.Formule;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.abonnement.StatutAbonnement;
import com.samanatteu.exception.abonnement.AbonnementInactifException;
import com.samanatteu.exception.abonnement.FormuleIntrouvableException;
import com.samanatteu.exception.abonnement.FormuleNonDisponibleException;
import com.samanatteu.exception.abonnement.QuotaDepasseException;
import com.samanatteu.exception.utilisateur.UtilisateurIntrouvableException;
import com.samanatteu.repository.abonnement.AbonnementRepository;
import com.samanatteu.repository.abonnement.FormuleRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;

// Les abonnements des gestionnaires.
//
// Règle du projet : l'ADMIN voit les abonnements (données commerciales de la
// plateforme) ; la gestionnaire voit le SIEN. Le filtrage est ici, dans le
// service — pas dans SecurityConfig, qui ne filtre que par rôle.
@Service
public class AbonnementService {

    public static final int DUREE_ESSAI_JOURS = 30;

    private final AbonnementRepository abonnementRepository;
    private final FormuleRepository formuleRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AbonnementService(AbonnementRepository abonnementRepository,
            FormuleRepository formuleRepository,
            UtilisateurRepository utilisateurRepository) {
        this.abonnementRepository = abonnementRepository;
        this.formuleRepository = formuleRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // La règle : une gestionnaire doit avoir un abonnement ACTIF pour
    // commencer une nouvelle activité (créer une tontine, inviter, importer).
    //
    // Les tontines DÉJÀ créées ne sont pas bloquées : les membres ont de
    // l'argent en jeu, et les couper créerait des impasses.
    public Abonnement exigerAbonnementActif(String telephone) {
        Abonnement abonnement = abonnementRepository.findByUtilisateurTelephone(telephone)
                .orElseThrow(() -> new AbonnementInactifException(telephone));
        if (!abonnement.estActif()) {
            throw new AbonnementInactifException(telephone);
        }
        return abonnement;
    }

    // Le quota de tontines de la formule. La formule MAX n'en a pas.
    public void verifierQuotaTontines(Abonnement abonnement, int nbTontinesActuelles) {
        Formule formule = abonnement.getFormule();
        if (formule.estIllimitee()) {
            return;
        }
        if (nbTontinesActuelles >= formule.getNbTontinesMax()) {
            throw new QuotaDepasseException("La formule " + formule.getNom() + " autorise "
                    + formule.getNbTontinesMax() + " tontine(s) : quota atteint.");
        }
    }

    // L'ADMIN active un abonnement : c'est l'activation MANUELLE (pas de paiement
    // réel pour l'instant), qui permet de développer et de tester sans prestataire
    // de paiement. Si la cliente a déjà un abonnement, il est prolongé.
    public AbonnementDTO activer(ActivationAbonnementDTO dto) {
        Formule formule = formuleRepository.findById(dto.getFormuleId())
                .orElseThrow(() -> new FormuleIntrouvableException(dto.getFormuleId()));
        if (!formule.getActive()) {
            throw new FormuleNonDisponibleException(formule.getNom());
        }
        Utilisateur cliente = utilisateurRepository.findById(dto.getUtilisateurId())
                .orElseThrow(() -> new UtilisateurIntrouvableException(dto.getUtilisateurId()));

        Abonnement abonnement = abonnementRepository.findByUtilisateurId(cliente.getId())
                .orElseGet(Abonnement::new);
        abonnement.setUtilisateur(cliente);
        abonnement.setFormule(formule);
        abonnement.setStatut(StatutAbonnement.ACTIF);
        abonnement.setDateDebut(LocalDate.now());
        // dateFin = le DERNIER JOUR couvert : avec 1 mois et un début le 20,
        // c'est le 19 du mois suivant (le 20 inclus, on ne perd pas un jour).
        abonnement.setDateFin(LocalDate.now().plusMonths(dto.getDureeMois()).minusDays(1));
        abonnement.setSmsEnvoyesMois(0);
        abonnement.setMoisCompteur(LocalDate.now().getMonthValue());

        return convertir(abonnementRepository.save(abonnement));
    }

    // L'essai gratuit, démarré quand la gestionnaire clique le lien de vérification :
    // mêmes droits qu'ACTIF, sur la formule choisie à l'inscription.
    public void demarrerEssai(Utilisateur cliente, Formule formule) {
        Abonnement abonnement = abonnementRepository.findByUtilisateurId(cliente.getId())
                .orElseGet(Abonnement::new);
        abonnement.setUtilisateur(cliente);
        abonnement.setFormule(formule);
        abonnement.setStatut(StatutAbonnement.ESSAI);
        abonnement.setDateDebut(LocalDate.now());
        // Même règle que activer() : dateFin est le DERNIER jour couvert, donc
        // 30 jours = aujourd'hui + 29.
        abonnement.setDateFin(LocalDate.now().plusDays(DUREE_ESSAI_JOURS - 1));
        abonnement.setSmsEnvoyesMois(0);
        abonnement.setMoisCompteur(LocalDate.now().getMonthValue());
        abonnementRepository.save(abonnement);
    }

    // L'abonnement d'une cliente (pour la route « mon abonnement »).
    public Optional<AbonnementDTO> monAbonnement(String telephone) {
        return abonnementRepository.findByUtilisateurTelephone(telephone).map(this::convertir);
    }

    // L'abonnement d'une cliente, par son id (fiche gestionnaire de l'ADMIN).
    public Optional<AbonnementDTO> abonnementDe(Long utilisateurId) {
        return abonnementRepository.findByUtilisateurId(utilisateurId).map(this::convertir);
    }

    // Tous les abonnements : pour l'ADMIN.
    public List<AbonnementDTO> lister() {
        return abonnementRepository.findAllByOrderByDateFinAsc().stream()
                .map(this::convertir)
                .toList();
    }

    private AbonnementDTO convertir(Abonnement abonnement) {
        AbonnementDTO dto = new AbonnementDTO();
        dto.setId(abonnement.getId());
        // Qui est la cliente : c'est l'ADMIN qui regarde cette liste.
        Utilisateur cliente = abonnement.getUtilisateur();
        dto.setUtilisateurId(cliente.getId());
        dto.setNom(cliente.getNom());
        dto.setPrenom(cliente.getPrenom());
        dto.setTelephone(cliente.getTelephone());
        dto.setEmail(cliente.getEmail());

        Formule formule = abonnement.getFormule();
        dto.setFormuleId(formule.getId());
        dto.setFormule(formule.getNom());
        dto.setPrixMensuel(formule.getPrixMensuel());
        dto.setNbTontinesMax(formule.getNbTontinesMax());
        dto.setNbMembresMax(formule.getNbMembresMax());
        dto.setSmsParMois(formule.getSmsParMois());

        dto.setStatut(abonnement.getStatut().name());
        dto.setActif(abonnement.estActif());
        dto.setDateDebut(abonnement.getDateDebut());
        dto.setDateFin(abonnement.getDateFin());
        dto.setSmsEnvoyesMois(abonnement.getSmsEnvoyesMois());
        return dto;
    }
}
