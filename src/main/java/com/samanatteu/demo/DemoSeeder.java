package com.samanatteu.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.samanatteu.dto.abonnement.ActivationAbonnementDTO;
import com.samanatteu.dto.cotisation.PaiementDTO;
import com.samanatteu.dto.cotisation.VersementDTO;
import com.samanatteu.dto.pret.DemandePretDTO;
import com.samanatteu.dto.tontine.CreationTontineDTO;
import com.samanatteu.dto.tontine.InscriptionDTO;
import com.samanatteu.dto.utilisateur.CreationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.enums.ModePaiement;
import com.samanatteu.enums.tontine.FrequenceTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.repository.abonnement.FormuleRepository;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.service.abonnement.AbonnementService;
import com.samanatteu.service.cotisation.CotisationService;
import com.samanatteu.service.cotisation.TirageService;
import com.samanatteu.service.pret.EcheancePretService;
import com.samanatteu.service.pret.PretService;
import com.samanatteu.service.tontine.CycleService;
import com.samanatteu.service.tontine.ParticipationService;
import com.samanatteu.service.tontine.TontineService;
import com.samanatteu.service.utilisateur.UtilisateurService;

import jakarta.persistence.EntityManager;

// Données de démonstration, créées au démarrage avec le profil « demo », et
// seulement si la base n'a aucun compte (jamais d'écrasement).
//
// Tout passe par les VRAIS services, connecté tour à tour comme l'admin puis
// chaque gestionnaire : mêmes règles que dans l'application (montants, cycles,
// caisse, tirages, mots de passe hachés, numéros en E.164). Les données sont
// donc cohérentes par construction.
//
// Les services datent tout d'« aujourd'hui » : la dernière étape (vieillir)
// recale les dates en SQL pour étaler l'histoire sur plusieurs mois.
//
// Vider avant de relancer : scripts/vider-donnees.sql.
@Component
@Profile("demo")
public class DemoSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoSeeder.class);

    // Le même pour tous les comptes : facile à donner pendant une présentation.
    static final String MOT_DE_PASSE = "Demo2026!";

    private final UtilisateurService utilisateurService;
    private final UtilisateurRepository utilisateurRepository;
    private final FormuleRepository formuleRepository;
    private final AbonnementService abonnementService;
    private final TontineService tontineService;
    private final ParticipationService participationService;
    private final CycleService cycleService;
    private final CotisationService cotisationService;
    private final CotisationRepository cotisationRepository;
    private final TirageService tirageService;
    private final PretService pretService;
    private final EcheancePretService echeancePretService;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;

    public DemoSeeder(UtilisateurService utilisateurService, UtilisateurRepository utilisateurRepository,
            FormuleRepository formuleRepository, AbonnementService abonnementService,
            TontineService tontineService, ParticipationService participationService,
            CycleService cycleService, CotisationService cotisationService,
            CotisationRepository cotisationRepository, TirageService tirageService, PretService pretService,
            EcheancePretService echeancePretService, JdbcTemplate jdbc, EntityManager entityManager,
            TransactionTemplate transaction) {
        this.utilisateurService = utilisateurService;
        this.utilisateurRepository = utilisateurRepository;
        this.formuleRepository = formuleRepository;
        this.abonnementService = abonnementService;
        this.tontineService = tontineService;
        this.participationService = participationService;
        this.cycleService = cycleService;
        this.cotisationService = cotisationService;
        this.cotisationRepository = cotisationRepository;
        this.tirageService = tirageService;
        this.pretService = pretService;
        this.echeancePretService = echeancePretService;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.transaction = transaction;
    }

    @Override
    public void run(String... args) {
        if (utilisateurRepository.count() > 0) {
            logger.warn("Seed de démo ignoré : la base contient déjà des comptes "
                    + "(la vider d'abord avec scripts/vider-donnees.sql).");
            return;
        }
        // Une seule transaction : si une étape échoue, RIEN n'est écrit (pas
        // de démo à moitié créée).
        try {
            transaction.executeWithoutResult(statut -> remplir());
        } finally {
            SecurityContextHolder.clearContext();
        }
        logger.info("Seed de démo terminé : {} comptes (mot de passe commun : {}).",
                utilisateurRepository.count(), MOT_DE_PASSE);
    }

    private void remplir() {
        // ---------- La plateforme : un admin, trois gestionnaires abonnées ----------
        // Le tout premier ADMIN ne peut être créé que par un ADMIN : le seeder
        // se présente comme tel le temps de cette seule création.
        connecter("seeder", RoleUtilisateur.ADMIN);
        UtilisateurDTO admin = compte("Admin", "Sarr", "770000090", "admin@samanatteu.sn", RoleUtilisateur.ADMIN);
        connecter(admin.getTelephone(), RoleUtilisateur.ADMIN);

        UtilisateurDTO aissatou = compte("Aïssatou", "Ndiaye", "771000001", "aissatou.ndiaye@demo.sn",
                RoleUtilisateur.GESTIONNAIRE);
        UtilisateurDTO ousmane = compte("Ousmane", "Fall", "772000002", "ousmane.fall@demo.sn",
                RoleUtilisateur.GESTIONNAIRE);
        UtilisateurDTO mariama = compte("Mariama", "Ba", "773000003", "mariama.ba@demo.sn",
                RoleUtilisateur.GESTIONNAIRE);
        abonner(aissatou, "PRO");
        abonner(ousmane, "DECOUVERTE");
        abonner(mariama, "MAX");

        // ---------- Aïssatou (PRO) : trois tontines, dans trois états ----------
        connecter(aissatou.getTelephone(), RoleUtilisateur.GESTIONNAIRE);
        Long awa = membre("Awa", "Diop", "781000001").getId();
        Long khady = membre("Khady", "Sow", "781000002").getId();
        Long ndeyeFatou = membre("Ndèye Fatou", "Guèye", "781000003").getId();
        Long coumba = membre("Coumba", "Faye", "781000004").getId();
        Long mameDiarra = membre("Mame Diarra", "Thiam", "781000005").getId();
        Long rokhaya = membre("Rokhaya", "Seck", "781000006").getId();
        Long astou = membre("Astou", "Mbaye", "781000007").getId();
        Long bineta = membre("Bineta", "Diallo", "781000008").getId();
        Long adama = membre("Adama", "Cissé", "781000009").getId();
        Long seynabou = membre("Seynabou", "Kane", "781000010").getId();
        Long dieynaba = membre("Dieynaba", "Ly", "781000011").getId();
        Long mareme = membre("Marème", "Niang", "781000012").getId();

        // 1. Active depuis 4 mois, avec un retard, un paiement partiel et un prêt.
        Long hlm = tontine("Tontine des commerçantes HLM", 10000, 1000, FrequenceTontine.MOIS, 5,
                "Cotisation mensuelle des commerçantes du marché HLM.");
        inscrire(hlm, awa, 2);
        for (Long m : List.of(khady, ndeyeFatou, coumba, mameDiarra, rokhaya, astou, bineta)) {
            inscrire(hlm, m, 1);
        }
        tontineService.activerTontine(hlm);
        cycleComplet(hlm, Set.of());
        cycleComplet(hlm, Set.of());
        cycleComplet(hlm, Set.of(bineta));
        // Prêt sur la caisse : la 1re échéance (il y a 15 jours) est remboursée.
        Long pretKhady = pret(hlm, khady, 6000, 3, LocalDate.now().minusDays(15));
        rembourser(pretKhady, 2000);
        Long cycleHlm = cycleService.ouvrirCycle(hlm).getId();
        payer(cycleHlm, Set.of(bineta, mameDiarra), Map.of(rokhaya, 5000));

        // 2. Terminée : 4 semaines, 4 tirages, tout est versé. Awa et Khady y
        // étaient aussi : on voit que les membres se comptent en personnes.
        Long tabaski = tontine("Tontine Tabaski 2026", 5000, 0, FrequenceTontine.SEMAINE, null,
                "Épargne hebdomadaire pour les dépenses de la Tabaski.");
        for (Long m : List.of(awa, khady, adama, seynabou)) {
            inscrire(tabaski, m, 1);
        }
        tontineService.activerTontine(tabaski);
        for (int i = 0; i < 4; i++) {
            cycleComplet(tabaski, Set.of());
        }
        tontineService.cloturerTontine(tabaski);

        // 3. En préparation : des inscrits, pas encore lancée.
        Long rentree = tontine("Épargne rentrée scolaire", 15000, 0, FrequenceTontine.MOIS, 10,
                "Pour les fournitures et l'inscription des enfants en octobre.");
        for (Long m : List.of(dieynaba, mareme, coumba)) {
            inscrire(rentree, m, 1);
        }

        // ---------- Ousmane (DÉCOUVERTE, 1 tontine) : la tontine familiale ----------
        connecter(ousmane.getTelephone(), RoleUtilisateur.GESTIONNAIRE);
        Long ibrahima = membre("Ibrahima", "Fall", "782000001").getId();
        Long aminata = membre("Aminata", "Fall", "782000002").getId();
        Long cheikh = membre("Cheikh", "Fall", "782000003").getId();
        Long penda = membre("Penda", "Fall", "782000004").getId();
        Long abdou = membre("Abdou", "Fall", "782000005").getId();

        Long famille = tontine("Famille Fall", 5000, 500, FrequenceTontine.MOIS, 1,
                "La tontine de la famille, chaque début de mois.");
        for (Long m : List.of(ibrahima, aminata, cheikh, penda, abdou)) {
            inscrire(famille, m, 1);
        }
        tontineService.activerTontine(famille);
        cycleComplet(famille, Set.of());
        Long cycleFamille = cycleService.ouvrirCycle(famille).getId();
        payer(cycleFamille, Set.of(abdou), Map.of(penda, 2500));

        // ---------- Mariama (MAX) : un GIE, avec un prêt en retard ----------
        connecter(mariama.getTelephone(), RoleUtilisateur.GESTIONNAIRE);
        Long yacine = membre("Yacine", "Diouf", "783000001").getId();
        Long sokhna = membre("Sokhna", "Mbengue", "783000002").getId();
        Long ramatoulaye = membre("Ramatoulaye", "Sy", "783000003").getId();
        Long fatoumata = membre("Fatoumata", "Touré", "783000004").getId();
        Long aida = membre("Aïda", "Ndour", "783000005").getId();
        Long nafi = membre("Nafi", "Gaye", "783000006").getId();

        Long gie = tontine("GIE des femmes de Thiès", 25000, 2000, FrequenceTontine.MOIS, 3,
                "Groupement d'intérêt économique : cotisation et caisse de crédit.");
        for (Long m : List.of(yacine, sokhna, ramatoulaye, fatoumata, aida, nafi)) {
            inscrire(gie, m, 1);
        }
        tontineService.activerTontine(gie);
        cycleComplet(gie, Set.of());
        cycleComplet(gie, Set.of(nafi));
        // Accordé après les paiements du cycle 2 (la caisse a alors 22 000 F) ;
        // 1re échéance il y a 25 jours, rien de payé : le prêt est en retard.
        Long pretSokhna = pret(gie, sokhna, 15000, 3, LocalDate.now().minusDays(25));
        Long cycleGie = cycleService.ouvrirCycle(gie).getId();
        payer(cycleGie, Set.of(sokhna, aida, nafi, fatoumata), Map.of());

        // Le job de minuit : les échéances dépassées passent EN_RETARD.
        echeancePretService.marquerRetard();

        // ---------- La chronologie ----------
        // Les services ont tout daté d'aujourd'hui. flush() : Hibernate écrit
        // en base ce qu'il gardait en mémoire, AVANT les UPDATE SQL ci-dessous.
        entityManager.flush();
        vieillir(hlm, FrequenceTontine.MOIS, 12);
        vieillir(tabaski, FrequenceTontine.SEMAINE, 75);
        vieillir(famille, FrequenceTontine.MOIS, 8);
        vieillir(gie, FrequenceTontine.MOIS, 5);
        LocalDate ilYa3Jours = LocalDate.now().minusDays(3);
        jdbc.update("update tontine set created_at = ? where id = ?", ilYa3Jours.atTime(10, 0), rentree);
        jdbc.update("update participation set date_adhesion = ? where tontine_id = ?", ilYa3Jours, rentree);
        dater(pretKhady, LocalDate.now().minusDays(45), LocalDate.now().minusDays(15));
        dater(pretSokhna, LocalDate.now().minusDays(28), null);
        datersComptes();
    }

    // ---------------------------------------------------------------- comptes

    // Le seeder agit « comme » cette personne : les services lisent le
    // téléphone et le rôle de l'utilisateur connecté (UtilisateurConnecte).
    private void connecter(String telephone, RoleUtilisateur role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                telephone, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private UtilisateurDTO compte(String prenom, String nom, String telephone, String email, RoleUtilisateur role) {
        CreationUtilisateurDTO dto = new CreationUtilisateurDTO();
        dto.setPrenom(prenom);
        dto.setNom(nom);
        dto.setTelephone(telephone);
        dto.setEmail(email);
        dto.setMotDePasse(MOT_DE_PASSE);
        dto.setRole(role);
        return utilisateurService.createUtilisateur(dto);
    }

    // Un membre, créé par sa gestionnaire (connectée) ; pas d'email, comme la
    // plupart des membres réels.
    private UtilisateurDTO membre(String prenom, String nom, String telephone) {
        return compte(prenom, nom, telephone, null, RoleUtilisateur.MEMBRE);
    }

    private void abonner(UtilisateurDTO gestionnaire, String formule) {
        ActivationAbonnementDTO dto = new ActivationAbonnementDTO();
        dto.setUtilisateurId(gestionnaire.getId());
        dto.setFormuleId(formuleRepository.findByNom(formule).orElseThrow().getId());
        dto.setDureeMois(12);
        abonnementService.activer(dto);
    }

    // ---------------------------------------------------------------- tontines

    private Long tontine(String nom, int part, int caisse, FrequenceTontine frequence, Integer jour,
            String description) {
        CreationTontineDTO dto = new CreationTontineDTO();
        dto.setNom(nom);
        dto.setMontantPart(BigDecimal.valueOf(part));
        dto.setMontantCaissePret(BigDecimal.valueOf(caisse));
        dto.setFrequence(frequence);
        dto.setIntervalle(1);
        dto.setJourCotisation(jour);
        dto.setDescription(description);
        return tontineService.createTontine(dto).getId();
    }

    private void inscrire(Long tontineId, Long membreId, int parts) {
        InscriptionDTO dto = new InscriptionDTO();
        dto.setTontineId(tontineId);
        dto.setMembreId(membreId);
        dto.setNombreParts(parts);
        participationService.createParticipation(dto);
    }

    // Un cycle de bout en bout : ouvert, payé (sauf les impayés), clôturé (les
    // impayés passent EN_RETARD), puis tiré au sort.
    private void cycleComplet(Long tontineId, Set<Long> impayes) {
        Long cycleId = cycleService.ouvrirCycle(tontineId).getId();
        payer(cycleId, impayes, Map.of());
        cycleService.cloturerCycle(cycleId);
        tirageService.tirerAuSort(cycleId);
    }

    // Paie chaque cotisation du cycle en entier (part + caisse), sauf les
    // impayés ; « partiels » donne un montant plus petit pour certains membres.
    private void payer(Long cycleId, Set<Long> impayes, Map<Long, Integer> partiels) {
        List<Cotisation> cotisations = cotisationRepository.findByCycleId(cycleId);
        for (Cotisation cotisation : cotisations) {
            Long membreId = cotisation.getParticipation().getMembre().getId();
            if (impayes.contains(membreId)) {
                continue;
            }
            BigDecimal total = cotisation.getMontantDu().add(cotisation.getMontantCaisseDu());
            BigDecimal montant = partiels.containsKey(membreId)
                    ? BigDecimal.valueOf(partiels.get(membreId))
                    : total;
            // Les trois moyens de paiement, en alternance : l'écran les montre tous.
            ModePaiement mode = ModePaiement.values()[(int) (cotisation.getId() % 3)];
            PaiementDTO paiement = new PaiementDTO();
            paiement.setMontant(montant);
            paiement.setModePaiement(mode);
            paiement.setReference(mode == ModePaiement.CASH ? null : mode.name() + "-" + (48000 + cotisation.getId()));
            cotisationService.enregistrerPaiement(cotisation.getId(), paiement);
        }
    }

    // ---------------------------------------------------------------- prêts

    private Long pret(Long tontineId, Long membreId, int montant, int nbEcheances, LocalDate debutRemboursement) {
        DemandePretDTO dto = new DemandePretDTO();
        dto.setMembreId(membreId);
        dto.setMontant(BigDecimal.valueOf(montant));
        dto.setNbEcheances(nbEcheances);
        dto.setDateDebutRemboursement(debutRemboursement);
        return pretService.accorderPret(tontineId, dto).getId();
    }

    private void rembourser(Long pretId, int montant) {
        VersementDTO dto = new VersementDTO();
        dto.setMontant(BigDecimal.valueOf(montant));
        pretService.rembourser(pretId, dto);
    }

    // ---------------------------------------------------------------- dates

    // Étale les cycles d'une tontine dans le passé : le dernier a commencé il
    // y a « joursDepuisDernier » jours, chaque précédent une période plus tôt.
    // Les paiements, tirages, transactions et adhésions suivent leur cycle.
    private void vieillir(Long tontineId, FrequenceTontine frequence, int joursDepuisDernier) {
        List<Map<String, Object>> cycles = jdbc.queryForList(
                "select id, numero_cycle, statut from cycle where tontine_id = ? order by numero_cycle", tontineId);
        int dernier = cycles.size();
        LocalDate premierDebut = LocalDate.now();
        for (Map<String, Object> cycle : cycles) {
            Long id = ((Number) cycle.get("id")).longValue();
            int numero = ((Number) cycle.get("numero_cycle")).intValue();
            LocalDate debut = reculer(LocalDate.now().minusDays(joursDepuisDernier), frequence, dernier - numero);
            LocalDate finPrevue = reculer(debut, frequence, -1);
            boolean cloture = "CLOTURE".equals(cycle.get("statut"));
            if (numero == 1) {
                premierDebut = debut;
            }
            jdbc.update("update cycle set date_debut = ?, date_fin_prevue = ?, date_fin_reelle = ?, created_at = ?"
                    + " where id = ?", debut, finPrevue, cloture ? finPrevue : null, debut.atTime(9, 0), id);
            // Payé entre 1 et 4 jours après l'ouverture (avant aujourd'hui, même
            // pour le cycle en cours, ouvert il y a au moins 5 jours).
            jdbc.update("update cotisation set created_at = ?, date_paiement = case when date_paiement is null"
                    + " then null else cast(? as date) + (id % 4 + 1) * interval '1 day' + interval '10 hours' end"
                    + " where cycle_id = ?", debut.atTime(9, 0), debut, id);
            if (cloture) {
                // Tirage le lendemain de la fin du cycle, à 18 h.
                LocalDateTime tirage = finPrevue.plusDays(1).atTime(18, 0);
                // cast : dans un CASE, PostgreSQL ne devine pas seul le type du paramètre.
                jdbc.update("update tirage set date_tirage = ?, created_at = ?, date_versement = case when"
                        + " date_versement is null then null else cast(? as timestamp) end where cycle_id = ?",
                        tirage, tirage, tirage, id);
                jdbc.update("update transaction t set date_transaction = ?, created_at = ? from tirage ti"
                        + " where t.type = 'GAIN' and t.reference_id = ti.id and ti.cycle_id = ?", tirage, tirage, id);
            }
            // Une cotisation compensée par le gain n'a pas de date de paiement :
            // sa transaction prend celle du tirage (ou du début de cycle).
            jdbc.update("update transaction t set date_transaction = coalesce(c.date_paiement, ?), created_at ="
                    + " coalesce(c.date_paiement, ?) from cotisation c where t.type = 'COTISATION'"
                    + " and t.reference_id = c.id and c.cycle_id = ?",
                    finPrevue.plusDays(1).atTime(18, 0), finPrevue.plusDays(1).atTime(18, 0), id);
        }
        jdbc.update("update participation set date_adhesion = ? where tontine_id = ?",
                premierDebut.minusDays(10), tontineId);
        jdbc.update("update tontine set created_at = ? where id = ?", premierDebut.minusDays(14).atTime(11, 0),
                tontineId);
    }

    // n périodes plus tôt (n négatif : plus tard).
    private LocalDate reculer(LocalDate date, FrequenceTontine frequence, int n) {
        return switch (frequence) {
            case JOUR -> date.minusDays(n);
            case SEMAINE -> date.minusWeeks(n);
            case MOIS -> date.minusMonths(n);
        };
    }

    // La date d'accord d'un prêt, et celle de son remboursement s'il y en a eu un.
    private void dater(Long pretId, LocalDate accord, LocalDate remboursement) {
        jdbc.update("update pret set date_accord = ?, created_at = ? where id = ?", accord, accord.atTime(15, 0),
                pretId);
        jdbc.update("update transaction set date_transaction = ?, created_at = ? where type = 'PRET'"
                + " and reference_id = ?", accord.atTime(15, 0), accord.atTime(15, 0), pretId);
        if (remboursement != null) {
            LocalDateTime le = remboursement.atTime(16, 30);
            jdbc.update("update echeance_pret set date_paiement = ? where pret_id = ? and date_paiement is not null",
                    le, pretId);
            jdbc.update("update transaction t set date_transaction = ?, created_at = ? from pret p"
                    + " where t.type = 'REMBOURSEMENT' and t.tontine_id = p.tontine_id and t.membre_id = p.membre_id"
                    + " and p.id = ?", le, le, pretId);
        }
    }

    // Chaque compte existe avant ce qu'il a fait : un membre la veille de sa
    // 1re adhésion, une gestionnaire avant sa 1re tontine, l'admin avant tous.
    // L'abonnement d'une gestionnaire commence le jour de son inscription.
    private void datersComptes() {
        jdbc.update("update utilisateur u set created_at = s.d from (select membre_id, min(date_adhesion)"
                + " - interval '1 day' + interval '19 hours' as d from participation group by membre_id) s"
                + " where u.id = s.membre_id");
        jdbc.update("update utilisateur u set created_at = (select min(t.created_at) from tontine t"
                + " where t.gestionnaire_id = u.id) - interval '6 days' where u.role = 'GESTIONNAIRE'");
        jdbc.update("update utilisateur set created_at = (select min(created_at) from utilisateur)"
                + " - interval '20 days' where role = 'ADMIN'");
        jdbc.update("update abonnement a set date_debut = cast(u.created_at as date), date_fin ="
                + " cast(cast(u.created_at as date) + interval '12 months' - interval '1 day' as date)"
                + " from utilisateur u where u.id = a.utilisateur_id");
    }
}
