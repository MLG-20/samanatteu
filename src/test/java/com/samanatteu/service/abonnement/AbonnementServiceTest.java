package com.samanatteu.service.abonnement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

// Tests du modèle d'abonnement.
//
// Chaque test porte une RÈGLE (son nom la dit) : c'est la règle d'or du
// projet — un test qui échoue doit dire quelle règle a été cassée.
@ExtendWith(MockitoExtension.class)
class AbonnementServiceTest {

    private static final String TELEPHONE = "+221770000101";

    @Mock
    private AbonnementRepository abonnementRepository;
    @Mock
    private FormuleRepository formuleRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private AbonnementService abonnementService;

    // ------------------------------------------------------------------ aides

    // Une formule : 0 = sans plafond.
    private Formule formule(String nom, int nbTontinesMax) {
        Formule formule = new Formule();
        formule.setId(1L);
        formule.setNom(nom);
        formule.setNbTontinesMax(nbTontinesMax);
        formule.setNbMembresMax(30);
        formule.setSmsParMois(100);
        formule.setPrixMensuel(new BigDecimal("5000.00"));
        formule.setActive(true);
        return formule;
    }

    // Un abonnement actif, valable encore (date de fin dans le futur).
    private Abonnement abonnementActif(Formule formule) {
        Abonnement abonnement = new Abonnement();
        abonnement.setId(1L);
        abonnement.setUtilisateur(cliente());
        abonnement.setFormule(formule);
        abonnement.setStatut(StatutAbonnement.ACTIF);
        abonnement.setDateDebut(LocalDate.now().minusDays(1));
        abonnement.setDateFin(LocalDate.now().plusDays(1));
        abonnement.setSmsEnvoyesMois(0);
        abonnement.setMoisCompteur(LocalDate.now().getMonthValue());
        return abonnement;
    }

    // Un abonnement dont la période est dépassée.
    private Abonnement abonnementExpire(Formule formule) {
        Abonnement abonnement = abonnementActif(formule);
        abonnement.setDateFin(LocalDate.now().minusDays(1));
        return abonnement;
    }

    // Un abonnement suspendu par la plateforme.
    private Abonnement abonnementSuspendu(Formule formule) {
        Abonnement abonnement = abonnementActif(formule);
        abonnement.setStatut(StatutAbonnement.SUSPENDU);
        return abonnement;
    }

    private Utilisateur cliente() {
        Utilisateur cliente = new Utilisateur();
        cliente.setId(18L);
        cliente.setNom("Diop");
        cliente.setPrenom("Awa");
        cliente.setTelephone(TELEPHONE);
        cliente.setEmail("awa.diop@example.com");
        return cliente;
    }

    private ActivationAbonnementDTO activation(Long formuleId, int dureeMois) {
        ActivationAbonnementDTO dto = new ActivationAbonnementDTO();
        dto.setUtilisateurId(18L);
        dto.setFormuleId(formuleId);
        dto.setDureeMois(dureeMois);
        return dto;
    }

    // ------------------------------------------------- la règle : abonnement actif

    // Règle : une gestionnaire avec un abonnement actif peut agir.
    @Test
    void exigerAbonnementActif_unAbonnementActif_passe() {
        Abonnement abonnement = abonnementActif(formule("PRO", 10));
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE)).thenReturn(Optional.of(abonnement));

        assertEquals(abonnement, abonnementService.exigerAbonnementActif(TELEPHONE));
    }

    // Règle : une gestionnaire SANS abonnement ne peut pas agir.
    @Test
    void exigerAbonnementActif_aucunAbonnement_estRefuse() {
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE)).thenReturn(Optional.empty());

        assertThrows(AbonnementInactifException.class,
                () -> abonnementService.exigerAbonnementActif(TELEPHONE));
    }

    // Règle : un abonnement EXPIRÉ ne donne plus le droit d'agir (la période
    // est dépassée, même si le statut dit encore ACTIF).
    @Test
    void exigerAbonnementActif_abonnementExpire_estRefuse() {
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE))
                .thenReturn(Optional.of(abonnementExpire(formule("PRO", 10))));

        assertThrows(AbonnementInactifException.class,
                () -> abonnementService.exigerAbonnementActif(TELEPHONE));
    }

    // Règle : un abonnement SUSPENDU par la plateforme ne donne plus le droit
    // d'agir, même si sa période n'est pas dépassée.
    @Test
    void exigerAbonnementActif_abonnementSuspendu_estRefuse() {
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE))
                .thenReturn(Optional.of(abonnementSuspendu(formule("PRO", 10))));

        assertThrows(AbonnementInactifException.class,
                () -> abonnementService.exigerAbonnementActif(TELEPHONE));
    }

    // ------------------------------------------------------- la règle : les quotas

    // Règle : on peut créer une tontine tant qu'on est sous le quota.
    @Test
    void verifierQuotaTontines_sousLeQuota_passe() {
        Abonnement abonnement = abonnementActif(formule("DECOUVERTE", 1));

        assertDoesNotThrow(() -> abonnementService.verifierQuotaTontines(abonnement, 0));
    }

    // Règle : le quota est atteint quand on a DÉJÀ autant de tontines que la
    // formule en autorise (1 tontine pour DECOUVERTE).
    @Test
    void verifierQuotaTontines_quotaAtteint_estRefuse() {
        Abonnement abonnement = abonnementActif(formule("DECOUVERTE", 1));

        assertThrows(QuotaDepasseException.class,
                () -> abonnementService.verifierQuotaTontines(abonnement, 1));
    }

    // Règle : une formule SANS plafond (MAX : 0) ne bloque jamais.
    @Test
    void verifierQuotaTontines_formuleIllimitee_passeToujours() {
        Abonnement abonnement = abonnementActif(formule("MAX", 0));

        assertDoesNotThrow(() -> abonnementService.verifierQuotaTontines(abonnement, 500));
    }

    // ---------------------------------------------------- l'activation par l'ADMIN

    // Règle : l'activation crée l'abonnement, actif, sur la durée demandée.
    // La date de fin est le DERNIER JOUR couvert (le jour même reste actif).
    @Test
    void activer_creeUnAbonnementActif() {
        Formule formule = formule("PRO", 10);
        when(formuleRepository.findById(1L)).thenReturn(Optional.of(formule));
        when(utilisateurRepository.findById(18L)).thenReturn(Optional.of(cliente()));
        when(abonnementRepository.save(any(Abonnement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AbonnementDTO resultat = abonnementService.activer(activation(1L, 1));

        // utilisateurId = l'id de la CLIENTE (18), pas celui de l'abonnement.
        assertEquals(18L, resultat.getUtilisateurId());
        assertEquals("PRO", resultat.getFormule());
        assertTrue(resultat.getActif());
        assertEquals(LocalDate.now().plusMonths(1).minusDays(1), resultat.getDateFin());
    }

    // Règle : l'essai dure 30 jours (dateFin = aujourd'hui + 29, le dernier jour
    // couvert), sur la formule donnée, et ouvre les mêmes droits qu'ACTIF.
    @Test
    void demarrerEssai_creeUnEssaiDe30JoursSurLaFormule() {
        when(abonnementRepository.findByUtilisateurId(18L)).thenReturn(Optional.empty());

        abonnementService.demarrerEssai(cliente(), formule("PRO", 10));

        ArgumentCaptor<Abonnement> enregistre = ArgumentCaptor.forClass(Abonnement.class);
        verify(abonnementRepository).save(enregistre.capture());
        Abonnement essai = enregistre.getValue();
        assertEquals(StatutAbonnement.ESSAI, essai.getStatut());
        assertEquals("PRO", essai.getFormule().getNom());
        assertEquals(LocalDate.now(), essai.getDateDebut());
        assertEquals(LocalDate.now().plusDays(29), essai.getDateFin());
        assertTrue(essai.estActif());
    }

    // Règle : un essai en cours donne le droit d'agir, comme un abonnement payé.
    @Test
    void exigerAbonnementActif_unEssaiEnCours_passe() {
        Abonnement essai = abonnementActif(formule("PRO", 10));
        essai.setStatut(StatutAbonnement.ESSAI);
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE)).thenReturn(Optional.of(essai));

        assertDoesNotThrow(() -> abonnementService.exigerAbonnementActif(TELEPHONE));
    }

    // Règle : un essai terminé bloque, comme un abonnement expiré.
    @Test
    void exigerAbonnementActif_essaiTermine_estRefuse() {
        Abonnement essai = abonnementExpire(formule("PRO", 10));
        essai.setStatut(StatutAbonnement.ESSAI);
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE)).thenReturn(Optional.of(essai));

        assertThrows(AbonnementInactifException.class, () -> abonnementService.exigerAbonnementActif(TELEPHONE));
    }

    // Règle : l'ADMIN voit QUI est la cliente (nom, téléphone, email) : sans
    // ça, un tableau de bord d'abonnements est une liste d'ids.
    @Test
    void activer_renvoieLIdentiteDeLaCliente() {
        when(formuleRepository.findById(1L)).thenReturn(Optional.of(formule("PRO", 10)));
        when(utilisateurRepository.findById(18L)).thenReturn(Optional.of(cliente()));
        when(abonnementRepository.save(any(Abonnement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AbonnementDTO resultat = abonnementService.activer(activation(1L, 1));

        assertEquals("Diop", resultat.getNom());
        assertEquals("Awa", resultat.getPrenom());
        assertEquals(TELEPHONE, resultat.getTelephone());
        assertEquals("awa.diop@example.com", resultat.getEmail());
    }

    // Règle : les quotas et le prix affichés viennent de la FORMULE (une
    // donnée que l'ADMIN modifie), pas de valeurs codées en dur.
    @Test
    void activer_renvoieLesQuotasDeLaFormule() {
        Formule formule = formule("PRO", 10);
        formule.setPrixMensuel(new BigDecimal("25000.00"));
        when(formuleRepository.findById(1L)).thenReturn(Optional.of(formule));
        when(utilisateurRepository.findById(18L)).thenReturn(Optional.of(cliente()));
        when(abonnementRepository.save(any(Abonnement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AbonnementDTO resultat = abonnementService.activer(activation(1L, 1));

        assertEquals(10, resultat.getNbTontinesMax());
        assertEquals(30, resultat.getNbMembresMax());
        assertEquals(100, resultat.getSmsParMois());
        assertEquals(0, new BigDecimal("25000.00").compareTo(resultat.getPrixMensuel()));
    }

    // Règle : une formule retirée de la vente ne peut pas être activée.
    @Test
    void activer_formuleRetiree_estRefuse() {
        Formule formule = formule("PRO", 10);
        formule.setActive(false);
        when(formuleRepository.findById(1L)).thenReturn(Optional.of(formule));

        assertThrows(FormuleNonDisponibleException.class,
                () -> abonnementService.activer(activation(1L, 1)));

        verify(abonnementRepository, never()).save(any());
    }

    // Règle : une formule inconnue est un 404, pas un 500.
    @Test
    void activer_formuleInconnue_estRefuse() {
        when(formuleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FormuleIntrouvableException.class,
                () -> abonnementService.activer(activation(99L, 1)));
    }

    // Règle : une cliente inconnue est un 404 aussi.
    @Test
    void activer_clienteInconnue_estRefuse() {
        when(formuleRepository.findById(1L)).thenReturn(Optional.of(formule("PRO", 10)));
        when(utilisateurRepository.findById(18L)).thenReturn(Optional.empty());

        assertThrows(UtilisateurIntrouvableException.class,
                () -> abonnementService.activer(activation(1L, 1)));
    }

    // ------------------------------------------------------ la lecture de l'abonnement

    // Règle : « mon abonnement » renvoie celui de la cliente, ou rien (404).
    @Test
    void monAbonnement_renvoieCeluiDeLaCliente() {
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE))
                .thenReturn(Optional.of(abonnementActif(formule("PRO", 10))));

        assertTrue(abonnementService.monAbonnement(TELEPHONE).isPresent());
    }

    @Test
    void monAbonnement_sansAbonnement_estVide() {
        when(abonnementRepository.findByUtilisateurTelephone(TELEPHONE)).thenReturn(Optional.empty());

        assertTrue(abonnementService.monAbonnement(TELEPHONE).isEmpty());
    }

    // Règle : l'ADMIN voit tous les abonnements.
    @Test
    void lister_renvoieTousLesAbonnements() {
        when(abonnementRepository.findAllByOrderByDateFinAsc())
                .thenReturn(List.of(abonnementActif(formule("PRO", 10))));

        assertEquals(1, abonnementService.lister().size());
    }
}
