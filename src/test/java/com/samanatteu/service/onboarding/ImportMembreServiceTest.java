package com.samanatteu.service.onboarding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.dto.onboarding.ImportMembreDTO;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.onboarding.StatutImportMembre;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.onboarding.FichierIllisibleException;
import com.samanatteu.exception.onboarding.FichierVideException;
import com.samanatteu.exception.tontine.InscriptionsFermeesException;
import com.samanatteu.exception.tontine.ParticipationDejaExistanteException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.repository.onboarding.ImportMembreRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.tontine.ParticipationService;

// Tests des règles d'ImportMembreService avec de faux repositories (Mockito).
// Convention : la tontine 6 appartient au gestionnaire 770000101 ; 770000102 est un
// autre gestionnaire. Les règles d'inscription elles-mêmes (doublon, parts, ordre)
// sont vérifiées par ParticipationServiceTest : ici ParticipationService est un faux,
// on vérifie seulement que l'import l'APPELLE avec les bonnes valeurs.
@ExtendWith(MockitoExtension.class)
class ImportMembreServiceTest {

    @Mock
    private ImportMembreRepository importMembreRepository;
    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ParticipationService participationService;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private ImportMembreService importMembreService;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ aides

    private static final String EN_TETE = "nom,prenom,telephone,email,parts\n";

    private void connecter(String telephone) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(telephone, null,
                        List.of(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))));
    }

    private Tontine tontine6(StatutTontine statut) {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setId(18L);
        gestionnaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setNom("Natt des femmes");
        tontine.setGestionnaire(gestionnaire);
        tontine.setStatut(statut);
        return tontine;
    }

    private MockMultipartFile csv(String contenu) {
        return new MockMultipartFile("fichier", "membres.csv", "text/csv",
                contenu.getBytes(StandardCharsets.UTF_8));
    }

    // Classeur Excel fabriqué en mémoire. Chaque tableau est une ligne ; null = ligne
    // absente du fichier (ce qu'Excel fait d'une ligne vide). Un nombre devient une
    // case numérique, comme un téléphone saisi dans un tableur.
    private MockMultipartFile xlsx(String nomFichier, Object[]... lignes) {
        try (Workbook classeur = new XSSFWorkbook();
                ByteArrayOutputStream octets = new ByteArrayOutputStream()) {
            Sheet feuille = classeur.createSheet();
            for (int i = 0; i < lignes.length; i++) {
                if (lignes[i] == null) {
                    continue;
                }
                Row rangee = feuille.createRow(i);
                for (int j = 0; j < lignes[i].length; j++) {
                    if (lignes[i][j] instanceof Number nombre) {
                        rangee.createCell(j).setCellValue(nombre.doubleValue());
                    } else {
                        rangee.createCell(j).setCellValue((String) lignes[i][j]);
                    }
                }
            }
            classeur.write(octets);
            return new MockMultipartFile("fichier", nomFichier,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", octets.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final Object[] EN_TETE_EXCEL = { "nom", "prenom", "telephone", "email", "parts" };

    // Cas courant : la gestionnaire 770000101 importe dans SA tontine 6, encore EN_ATTENTE.
    // Les faux repositories « enregistrent » en renvoyant l'objet reçu.
    private Tontine preparerImportAutorise() {
        connecter("770000101");
        Tontine tontine = tontine6(StatutTontine.EN_ATTENTE);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        when(importMembreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return tontine;
    }

    // ------------------------------------------- refus avant de lire le fichier

    @Test
    void importer_tontineInconnue_donneTontineIntrouvable() {
        connecter("770000101");
        when(tontineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TontineIntrouvableException.class,
                () -> importMembreService.importer(99L, csv(EN_TETE + "Diop,Awa,771110001,,1\n")));

        verify(importMembreRepository, never()).save(any());
    }

    @Test
    void importer_parUnAutreGestionnaire_estRefuseSansRienCreer() {
        connecter("770000102");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));

        assertThrows(AccesRefuseException.class,
                () -> importMembreService.importer(6L, csv(EN_TETE + "Diop,Awa,771110001,,1\n")));

        verifyNoInteractions(utilisateurRepository, participationService);
        verify(importMembreRepository, never()).save(any());
    }

    // Vérifié AVANT la boucle : sinon chaque ligne créerait un compte avant d'être
    // refusée par l'inscription (des comptes qui ne participent à rien).
    @ParameterizedTest
    @EnumSource(value = StatutTontine.class, names = "EN_ATTENTE", mode = EnumSource.Mode.EXCLUDE)
    void importer_inscriptionsFermees_estRefuseSansCreerDeCompte(StatutTontine statut) {
        connecter("770000101");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(statut)));

        assertThrows(InscriptionsFermeesException.class,
                () -> importMembreService.importer(6L, csv(EN_TETE + "Diop,Awa,771110001,,1\n")));

        verifyNoInteractions(utilisateurRepository, participationService);
        verify(importMembreRepository, never()).save(any());
    }

    // ------------------------------------------------------- fichier inutilisable

    // Fichier vide, en-tête seul, ou en-tête suivi seulement de lignes vides :
    // aucun rapport enregistré (pas de « -1 membre » ni de « 0 membre »).
    @ParameterizedTest
    @ValueSource(strings = { "", "nom,prenom,telephone,email,parts\n",
            "nom,prenom,telephone,email,parts\n\n\n" })
    void importer_fichierSansMembre_donneFichierVide(String contenu) {
        connecter("770000101");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));

        assertThrows(FichierVideException.class, () -> importMembreService.importer(6L, csv(contenu)));

        verify(importMembreRepository, never()).save(any());
    }

    // Guillemet ouvert jamais refermé : OpenCSV échoue, l'erreur technique devient un 400.
    @Test
    void importer_csvMalForme_donneFichierIllisible() {
        connecter("770000101");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));

        assertThrows(FichierIllisibleException.class,
                () -> importMembreService.importer(6L, csv("nom,prenom,telephone\n\"Diop,Awa,771110001\n")));

        verify(importMembreRepository, never()).save(any());
    }

    // ------------------------------------------------------------ import partiel

    // Le fichier d'exemple (exemples/membres.csv) : 3 lignes bonnes, 3 fausses.
    @Test
    void importer_fichierMixte_importeLesBonnesLignesEtExpliqueLesAutres() {
        Tontine tontine = preparerImportAutorise();
        when(utilisateurRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportMembreDTO rapport = importMembreService.importer(6L, csv(EN_TETE
                + "Diop,Awa,771110001,awa.diop@example.com,2\n"
                + "Ndiaye,Fatou,771110002,,1\n"
                + "Sow,Mariama,771110003,,\n"
                + "Fall,Khady,77111,,1\n"
                + ",Aminata,771110005,,1\n"
                + "Diop,Awa,771110001,,1\n"));

        assertEquals(6, rapport.getNbMembresTotal());
        assertEquals(3, rapport.getNbImportes());
        assertEquals(3, rapport.getNbErreurs());
        assertEquals(StatutImportMembre.TERMINE, rapport.getStatut());
        assertEquals("membres.csv", rapport.getFichierNom());
        assertEquals(6L, rapport.getTontineId());
        // Numéro de ligne tel qu'on le voit dans un tableur (l'en-tête est la ligne 1).
        assertEquals("Ligne 5 : telephone invalide (9 chiffres commençant par 7)\n"
                + "Ligne 6 : nom manquant\n"
                + "Ligne 7 : telephone en double dans le fichier\n", rapport.getErreursDetail());

        // Parts lues dans le fichier (2), ou 1 par défaut si la colonne est vide.
        ArgumentCaptor<Utilisateur> membres = ArgumentCaptor.forClass(Utilisateur.class);
        ArgumentCaptor<Integer> parts = ArgumentCaptor.forClass(Integer.class);
        verify(participationService, times(3)).inscrire(eq(tontine), membres.capture(), parts.capture());
        assertEquals(List.of(2, 1, 1), parts.getAllValues());
        assertEquals(List.of("771110001", "771110002", "771110003"),
                membres.getAllValues().stream().map(Utilisateur::getTelephone).toList());
    }

    // Décision A : le compte est créé par l'import, MEMBRE et sans mot de passe
    // (personne ne peut s'y connecter tant que le membre ne l'a pas choisi).
    @Test
    void importer_telephoneInconnu_creeUnCompteMembreSansMotDePasse() {
        preparerImportAutorise();
        when(utilisateurRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        importMembreService.importer(6L, csv(EN_TETE + "Diop,Awa,771110001,awa.diop@example.com,2\n"));

        ArgumentCaptor<Utilisateur> cree = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(cree.capture());
        assertEquals("Diop", cree.getValue().getNom());
        assertEquals("Awa", cree.getValue().getPrenom());
        assertEquals("771110001", cree.getValue().getTelephone());
        assertEquals("awa.diop@example.com", cree.getValue().getEmail());
        assertEquals(RoleUtilisateur.MEMBRE, cree.getValue().getRole());
        assertNull(cree.getValue().getMotDePasse());
    }

    // Décision A : un compte existant est réutilisé tel quel, jamais recréé ni modifié.
    @Test
    void importer_telephoneDejaConnu_reutiliseLeCompteSansLeModifier() {
        Tontine tontine = preparerImportAutorise();
        Utilisateur existant = new Utilisateur();
        existant.setId(30L);
        existant.setNom("Diop");
        existant.setPrenom("Awa");
        existant.setTelephone("771110001");
        when(utilisateurRepository.findByTelephone("771110001")).thenReturn(Optional.of(existant));

        ImportMembreDTO rapport = importMembreService.importer(6L,
                csv(EN_TETE + "AutreNom,AutrePrenom,771110001,autre@example.com,3\n"));

        assertEquals(1, rapport.getNbImportes());
        verify(utilisateurRepository, never()).save(any());
        assertEquals("Diop", existant.getNom());
        assertNull(existant.getEmail());
        ArgumentCaptor<Utilisateur> inscrit = ArgumentCaptor.forClass(Utilisateur.class);
        verify(participationService).inscrire(eq(tontine), inscrit.capture(), eq(3));
        assertSame(existant, inscrit.getValue());
    }

    // Une inscription refusée (membre déjà dans la tontine) n'arrête pas l'import :
    // la ligne part dans le rapport avec le message du refus, la suivante est traitée.
    @Test
    void importer_inscriptionRefusee_noteLaLigneEtContinue() {
        Tontine tontine = preparerImportAutorise();
        Utilisateur dejaInscrite = new Utilisateur();
        dejaInscrite.setId(30L);
        dejaInscrite.setTelephone("771110001");
        Utilisateur nouvelle = new Utilisateur();
        nouvelle.setId(31L);
        nouvelle.setTelephone("771110002");
        when(utilisateurRepository.findByTelephone("771110001")).thenReturn(Optional.of(dejaInscrite));
        when(utilisateurRepository.findByTelephone("771110002")).thenReturn(Optional.of(nouvelle));
        when(participationService.inscrire(tontine, dejaInscrite, 1))
                .thenThrow(new ParticipationDejaExistanteException());

        ImportMembreDTO rapport = importMembreService.importer(6L,
                csv(EN_TETE + "Diop,Awa,771110001,,1\nNdiaye,Fatou,771110002,,1\n"));

        assertEquals(1, rapport.getNbImportes());
        assertEquals(1, rapport.getNbErreurs());
        assertEquals("Ligne 2 : Ce membre participe déjà à cette tontine\n", rapport.getErreursDetail());
        assertEquals(StatutImportMembre.TERMINE, rapport.getStatut());
        verify(participationService).inscrire(tontine, nouvelle, 1);
    }

    // Aucune ligne importée : le rapport est quand même enregistré, avec le statut ECHEC.
    @Test
    void importer_aucuneLigneValide_donneUnRapportEnEchec() {
        preparerImportAutorise();

        ImportMembreDTO rapport = importMembreService.importer(6L,
                csv(EN_TETE + "Fall,Khady,77111,,1\n,Aminata,771110005,,1\n"));

        assertEquals(0, rapport.getNbImportes());
        assertEquals(2, rapport.getNbErreurs());
        assertEquals(StatutImportMembre.ECHEC, rapport.getStatut());
        verifyNoInteractions(participationService);
        verify(utilisateurRepository, never()).save(any());
    }

    // -------------------------------------------------------- contrôle des lignes

    @Test
    void importer_emailDejaPris_noteLaLigneSansCreerLeCompte() {
        preparerImportAutorise();
        when(utilisateurRepository.existsByEmail("pris@example.com")).thenReturn(true);

        ImportMembreDTO rapport = importMembreService.importer(6L,
                csv(EN_TETE + "Sarr,Ndeye,772220003,pris@example.com,2\n"));

        assertEquals(0, rapport.getNbImportes());
        assertEquals("Ligne 2 : L'email pris@example.com est déjà utilisé.\n", rapport.getErreursDetail());
        verify(utilisateurRepository, never()).save(any());
        verifyNoInteractions(participationService);
    }

    // Une ligne par règle de verifierLigne ; rien n'est créé pour une ligne refusée.
    @Test
    void importer_lignesInvalides_donneLeMotifDeChacune() {
        preparerImportAutorise();

        ImportMembreDTO rapport = importMembreService.importer(6L, csv(EN_TETE
                + "Diop,Awa\n"
                + ",Awa,771110001,,1\n"
                + "Diop,,771110001,,1\n"
                + "Diop,Awa,661110001,,1\n"
                + "Diop,Awa,7711100011,,1\n"
                + "Faye,Coumba,772220002,pas-un-email,1\n"
                + "Diallo,Binta,772220004,,abc\n"
                + "Diallo,Binta,772220004,,0\n"));

        assertEquals(8, rapport.getNbErreurs());
        assertEquals("Ligne 2 : colonnes manquantes (nom, prenom, telephone attendus)\n"
                + "Ligne 3 : nom manquant\n"
                + "Ligne 4 : prenom manquant\n"
                + "Ligne 5 : telephone invalide (9 chiffres commençant par 7)\n"
                + "Ligne 6 : telephone invalide (9 chiffres commençant par 7)\n"
                + "Ligne 7 : email invalide\n"
                + "Ligne 8 : nombre de parts invalide (entier à partir de 1)\n"
                + "Ligne 9 : nombre de parts invalide (entier à partir de 1)\n", rapport.getErreursDetail());
        verify(utilisateurRepository, never()).save(any());
        verifyNoInteractions(participationService);
    }

    // Les colonnes email et parts sont optionnelles : 3 colonnes suffisent.
    @Test
    void importer_troisColonnesSeulement_importeAvecUnePart() {
        Tontine tontine = preparerImportAutorise();
        when(utilisateurRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportMembreDTO rapport = importMembreService.importer(6L,
                csv("nom,prenom,telephone\nDiop,Awa,771110001\n"));

        assertEquals(1, rapport.getNbImportes());
        assertTrue(rapport.getErreursDetail().isEmpty());
        verify(participationService).inscrire(eq(tontine), any(), eq(1));
    }

    // --------------------------------------------------------------- lignes vides

    // Une ligne vide est de la mise en page : ni importée, ni signalée, ni comptée
    // dans le total. La ligne suivante garde son numéro de tableur (4, pas 3).
    @Test
    void importer_csvAvecLigneVide_lIgnoreSansDecalerLesNumeros() {
        preparerImportAutorise();
        when(utilisateurRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportMembreDTO rapport = importMembreService.importer(6L,
                csv(EN_TETE + "Ba,Aissatou,771110010,,1\n\nFall,Khady,77111,,1\n"));

        assertEquals(2, rapport.getNbMembresTotal());
        assertEquals(1, rapport.getNbImportes());
        assertEquals(1, rapport.getNbErreurs());
        assertEquals("Ligne 4 : telephone invalide (9 chiffres commençant par 7)\n", rapport.getErreursDetail());
    }

    // ---------------------------------------------------------------------- Excel

    // Dans Excel le téléphone et les parts sont des NOMBRES : lus sans DataFormatter,
    // ils sortiraient en 7.71110001E8 et 2.0, et la ligne serait refusée.
    @ParameterizedTest
    @ValueSource(strings = { "membres.xlsx", "MEMBRES.XLSX" })
    void importer_excel_litLesCasesNumeriquesCommeDuTexte(String nomFichier) {
        Tontine tontine = preparerImportAutorise();
        when(utilisateurRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportMembreDTO rapport = importMembreService.importer(6L, xlsx(nomFichier,
                EN_TETE_EXCEL,
                new Object[] { "Diop", "Awa", 771110001, "awa.diop@example.com", 2 },
                new Object[] { "Ndiaye", "Fatou", "771110002" }));

        assertEquals(2, rapport.getNbMembresTotal());
        assertEquals(2, rapport.getNbImportes());
        assertTrue(rapport.getErreursDetail().isEmpty());
        assertEquals(nomFichier, rapport.getFichierNom());

        ArgumentCaptor<Utilisateur> membres = ArgumentCaptor.forClass(Utilisateur.class);
        ArgumentCaptor<Integer> parts = ArgumentCaptor.forClass(Integer.class);
        verify(participationService, times(2)).inscrire(eq(tontine), membres.capture(), parts.capture());
        assertEquals(List.of(2, 1), parts.getAllValues());
        assertEquals(List.of("771110001", "771110002"),
                membres.getAllValues().stream().map(Utilisateur::getTelephone).toList());
    }

    // Excel n'enregistre pas une ligne vide (null ici) : lue par for-each, elle serait
    // sautée et Khady serait annoncée en ligne 4. Une ligne aux cases toutes vides
    // (effacée mais encore mise en forme) est ignorée de la même façon.
    @Test
    void importer_excelAvecLignesVides_lesIgnoreSansDecalerLesNumeros() {
        preparerImportAutorise();
        when(utilisateurRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportMembreDTO rapport = importMembreService.importer(6L, xlsx("trous.xlsx",
                EN_TETE_EXCEL,
                new Object[] { "Ba", "Aissatou", 771110010, "", 1 },
                null,
                new Object[] { "", "", "" },
                new Object[] { "Fall", "Khady", 77111, "", 1 }));

        assertEquals(2, rapport.getNbMembresTotal());
        assertEquals(1, rapport.getNbImportes());
        assertEquals(1, rapport.getNbErreurs());
        assertEquals("Ligne 5 : telephone invalide (9 chiffres commençant par 7)\n", rapport.getErreursDetail());
    }

    // En-tête suivi seulement de lignes vides : même refus qu'un fichier vide.
    @Test
    void importer_excelSansMembre_donneFichierVide() {
        connecter("770000101");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));

        assertThrows(FichierVideException.class, () -> importMembreService.importer(6L,
                xlsx("vide.xlsx", EN_TETE_EXCEL, null, new Object[] { "", "", "" })));

        verify(importMembreRepository, never()).save(any());
    }

    // Un fichier texte renommé en .xlsx : POI lance une exception NON vérifiée, qui
    // doit devenir un 400 lisible et non un 500.
    @Test
    void importer_fauxExcel_donneFichierIllisible() {
        connecter("770000101");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));
        MockMultipartFile faux = new MockMultipartFile("fichier", "faux.xlsx", "text/csv",
                (EN_TETE + "Diop,Awa,771110001,,1\n").getBytes(StandardCharsets.UTF_8));

        assertThrows(FichierIllisibleException.class, () -> importMembreService.importer(6L, faux));

        verifyNoInteractions(utilisateurRepository, participationService);
        verify(importMembreRepository, never()).save(any());
    }

    // ------------------------------------------------------------------- lecture

    // Chaque gestionnaire ne lit que les rapports de ses tontines : filtre par le
    // téléphone du token, jamais findAll.
    @Test
    void listImportMembre_filtreParLeTelephoneDuToken() {
        connecter("770000101");
        when(importMembreRepository.findByTontineGestionnaireTelephoneOrderByCreatedAtDesc("770000101"))
                .thenReturn(List.of());

        importMembreService.listImportMembre();

        verify(importMembreRepository).findByTontineGestionnaireTelephoneOrderByCreatedAtDesc("770000101");
        verify(importMembreRepository, never()).findAll();
    }
}
