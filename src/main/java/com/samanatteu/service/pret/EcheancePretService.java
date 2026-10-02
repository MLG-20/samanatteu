package com.samanatteu.service.pret;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.pret.EcheancePretDTO;
import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.notification.TypeNotification;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.notification.NotificationService;

@Service
public class EcheancePretService {
    private final EcheancePretRepository echeancePretRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final PretRepository pretRepository;
    private final NotificationService notificationService;
    // 05/10/2026 plutôt que 2026-10-05 : lisible pour les membres.
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public EcheancePretService(EcheancePretRepository echeancePretRepository, UtilisateurConnecte utilisateurConnecte,
            PretRepository pretRepository, NotificationService notificationService) {
        this.echeancePretRepository = echeancePretRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.pretRepository = pretRepository;
        this.notificationService = notificationService;
    }

    // Même filtre que listPret, en passant par le prêt de l'échéance.
    public List<EcheancePretDTO> listEcheancePret() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);
        List<EcheancePret> echeances = estGestionnaire
                ? echeancePretRepository.findByPretTontineGestionnaireTelephone(utilisateurConnecte.telephone())
                : echeancePretRepository.findByPretMembreTelephone(utilisateurConnecte.telephone());

        return echeances.stream()
                .map(this::convertiEcheancePretDTO)
                .toList();
    }

    // Chaque nuit à minuit, lancée par Spring (aucun utilisateur connecté,
    // donc pas de verifierGestionnaire ; jamais exposée par une route).
    // Échéances EN_ATTENTE dépassées → EN_RETARD, et leur prêt aussi.
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void marquerRetard() {
        List<EcheancePret> depassees = echeancePretRepository
                .findByStatutAndDateEcheanceBefore(StatutEcheancePret.EN_ATTENTE, LocalDate.now());

        for (EcheancePret echeance : depassees) {
            echeance.setStatut(StatutEcheancePret.EN_RETARD);
            echeancePretRepository.save(echeance);

            Pret pret = echeance.getPret();
            if (pret.getStatut() == StatutPret.ACTIF) {
                pret.setStatut(StatutPret.EN_RETARD);
                pretRepository.save(pret);
            }
        }
    }

    // Rappel J-3 (CDC §3.8), chaque jour à 9h (pas minuit : un SMS réveille).
    // Égalité sur la date (pas « avant ») : un seul rappel par échéance.
    // Reste dû et non montantDu : une partie a pu être payée en avance.
    @Scheduled(cron = "0 0 9 * * *")
    public void rappelerEcheances() {
        List<EcheancePret> proches = echeancePretRepository
                .findByStatutAndDateEcheance(StatutEcheancePret.EN_ATTENTE, LocalDate.now().plusDays(3));
        for (EcheancePret echeance : proches) {
            Pret pret = echeance.getPret();
            Utilisateur membre = pret.getMembre();
            BigDecimal reste = echeance.getMontantDu().subtract(echeance.getMontantPaye());
            notificationService.notifier(membre, TypeNotification.RAPPEL_ECHEANCE,
                    pret.getTontine().getNom() + " : " + membre.getPrenom() + " " + membre.getNom()
                            + ", rappel : échéance de " + reste.stripTrailingZeros().toPlainString() + " F de votre prêt le "
                            + echeance.getDateEcheance().format(FORMAT_DATE) + ".");
        }
    }


    private EcheancePretDTO convertiEcheancePretDTO(EcheancePret echeancePret) {
        EcheancePretDTO dto = new EcheancePretDTO();
        dto.setId(echeancePret.getId());
        dto.setPretId(echeancePret.getPret().getId());
        dto.setNumeroEcheance(echeancePret.getNumeroEcheance());
        dto.setMontantDu(echeancePret.getMontantDu());
        dto.setMontantPaye(echeancePret.getMontantPaye());
        dto.setDateEcheance(echeancePret.getDateEcheance());
        dto.setDatePaiement(echeancePret.getDatePaiement());
        dto.setStatut(echeancePret.getStatut());

        return dto;
    }
}
