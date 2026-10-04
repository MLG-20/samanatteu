package com.samanatteu.service.onboarding;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.samanatteu.dto.onboarding.ImportMembreDTO;
import com.samanatteu.entity.onboarding.ImportMembre;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.onboarding.StatutImportMembre;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.SamanatteuException;
import com.samanatteu.exception.onboarding.FichierIllisibleException;
import com.samanatteu.exception.onboarding.FichierVideException;
import com.samanatteu.exception.tontine.InscriptionsFermeesException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.exception.utilisateur.EmailDejaUtiliseException;
import com.samanatteu.repository.onboarding.ImportMembreRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.tontine.ParticipationService;

@Service
public class ImportMembreService {
    private final ImportMembreRepository importMembreRepository;
    private final TontineRepository tontineRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final UtilisateurRepository utilisateurRepository;
    private final ParticipationService participationService;

    public ImportMembreService(ImportMembreRepository importMembreRepository, UtilisateurConnecte utilisateurConnecte,
            TontineRepository tontineRepository, UtilisateurRepository utilisateurRepository,
            ParticipationService participationService) {
        this.importMembreRepository = importMembreRepository;
        this.utilisateurConnecte = utilisateurConnecte;
        this.tontineRepository = tontineRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.participationService = participationService;
    }

    // Le téléphone vient du token (jamais du client) : chaque gestionnaire
    // ne lit que les rapports de ses propres tontines.
    public List<ImportMembreDTO> listImportMembre() {
        return importMembreRepository
                .findByTontineGestionnaireTelephoneOrderByCreatedAtDesc(utilisateurConnecte.telephone())
                .stream()
                .map(this::convertiImportMembreDTO)
                .toList();
    }

    // D'abord « qui a le droit » (404 puis 403), avant de toucher au fichier.
    public ImportMembreDTO importer(Long tontineId, MultipartFile fichier) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new TontineIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tontine);

        // Vérifié une fois, avant la boucle : sinon chaque ligne créerait un compte
        // avant d'être refusée par inscrire (des comptes qui ne participent à rien).
        if (tontine.getStatut() != StatutTontine.EN_ATTENTE) {
            throw new InscriptionsFermeesException(tontine.getStatut().toString());
        }

        // Le lecteur est choisi d'après l'extension ; tout ce qui n'est pas .xlsx est
        // lu comme un CSV. Le nom peut être null (client qui n'en envoie pas).
        List<String[]> lignes;
        String nom = fichier.getOriginalFilename();
        if (nom != null && nom.toLowerCase().endsWith(".xlsx")) {
            lignes = lireLignesExcel(fichier);
        } else {
            lignes = lireLignes(fichier);
        }

        // 0 ligne (fichier vide) ou 1 ligne (en-tête seul) : rien à importer.
        if (lignes.size() < 2) {
            throw new FichierVideException();
        }

        int nbErreurs = 0;
        int nbImport = 0;
        int nbVides = 0;
        StringBuilder detail = new StringBuilder();

        Set<String> telephoneVus = new HashSet<>();

        // i part de 1 pour sauter l'en-tête. Une ligne fausse n'arrête pas l'import
        // (import partiel) : elle est comptée et expliquée dans le rapport, avec
        // i + 1 = son numéro tel qu'on le voit dans un tableur.
        for (int i = 1; i < lignes.size(); i++) {
            String[] ligne = lignes.get(i);
            // Ligne vide : ni importée ni signalée. continue passe au tour suivant
            // sans exécuter la suite ; elle garde son numéro, seul le total change.
            if (estVide(ligne)) {
                nbVides++;
                continue;
            }
            String erreur = verifierLigne(ligne);
            // Doublon dans le fichier : add() répond false si le téléphone a déjà été vu.
            // Placé avant le comptage, pour que le doublon soit compté comme les autres erreurs.
            if (erreur == null && !telephoneVus.add(ligne[2])) {
                erreur = "telephone en double dans le fichier";
            }
            if (erreur != null) {
                nbErreurs++;
                detail.append("Ligne ").append(i + 1).append(" : ").append(erreur).append("\n");
            } else {
                // Un refus métier (déjà inscrit, inscriptions fermées…) ne doit pas arrêter
                // tout l'import : SamanatteuException, mère de toutes les exceptions métier,
                // les rattrape toutes, et la ligne part dans le rapport avec son message.
                try {
                    Utilisateur membre = trouverOuCreerMembre(ligne);
                    participationService.inscrire(tontine, membre, lireParts(ligne));
                    nbImport++;
                } catch (SamanatteuException e) {
                    nbErreurs++;
                    detail.append("Ligne ").append(i + 1).append(" : ").append(e.getMessage()).append("\n");
                }
            }

        }

        // Calculé après la boucle : avant, nbVides vaudrait encore 0.
        // - 1 : la première ligne est l'en-tête (nom,prenom,…), pas un membre.
        // - nbVides : une ligne vide est de la mise en page, pas un membre raté.
        // 0 = en-tête suivi seulement de lignes vides : même refus qu'un fichier vide.
        int nbMembres = lignes.size() - 1 - nbVides;
        if (nbMembres == 0) {
            throw new FichierVideException();
        }

        // Le rapport est rempli par le serveur à partir de ce qu'il a compté :
        // le client ne fournit que le fichier.
        ImportMembre importMembre = new ImportMembre();
        importMembre.setTontine(tontine);
        importMembre.setFichierNom(fichier.getOriginalFilename());
        importMembre.setNbMembresTotal(nbMembres);
        importMembre.setNbImportes(nbImport);
        importMembre.setNbErreurs(nbErreurs);
        importMembre.setErreursDetail(detail.toString());

        // Le rapport est enregistré une fois l'import fini : TERMINE si au moins une
        // ligne est passée, ECHEC si aucune (la gestionnaire le voit dans sa liste).
        if (nbImport > 0) {
            importMembre.setStatut(StatutImportMembre.TERMINE);
        } else {
            importMembre.setStatut(StatutImportMembre.ECHEC);
        }

        importMembre.setCreatedAt(LocalDateTime.now());

        ImportMembre enregistre = importMembreRepository.save(importMembre);
        return convertiImportMembreDTO(enregistre);
    }

    // Octets → caractères (UTF-8, pour les accents) → lignes découpées en cases.
    // try-with-resources : le lecteur est fermé même si la lecture échoue.
    // Une erreur technique de lecture devient un 400 lisible pour le client.
    private List<String[]> lireLignes(MultipartFile fichier) {
        try (CSVReader lecteur = new CSVReader(
                new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {
            return lecteur.readAll();
        } catch (IOException | CsvException e) {
            throw new FichierIllisibleException();
        }
    }

    // Même résultat que lireLignes (une liste de lignes découpées en cases), pour que
    // le reste de l'import ne sache pas si le fichier était un CSV ou un Excel.
    // Le classeur est fermé par le try-with-resources ; getSheetAt(0) = 1re feuille.
    private List<String[]> lireLignesExcel(MultipartFile fichier) {
        try (Workbook classeur = new XSSFWorkbook(fichier.getInputStream())) {
            Sheet feuille = classeur.getSheetAt(0);
            List<String[]> lignes = new ArrayList<>();

            // Rend le texte d'une case tel qu'Excel l'affiche : sans lui, un téléphone
            // (stocké comme un nombre) sortirait en 7.71234566E8. Case vide = "".
            DataFormatter formateur = new DataFormatter();

            // Parcours par numéro et non par for-each : Excel n'enregistre pas une ligne
            // vide, un for-each la sauterait et décalerait les numéros du rapport.
            // La ligne vide garde donc sa place, sous forme d'un tableau sans case.
            for (int i = 0; i <= feuille.getLastRowNum(); i++) {
                Row rangee = feuille.getRow(i);
                if (rangee == null || rangee.getLastCellNum() < 0) {
                    lignes.add(new String[0]);
                } else {
                    String[] cases = new String[rangee.getLastCellNum()];
                    for (int j = 0; j < cases.length; j++) {
                        cases[j] = formateur.formatCellValue(rangee.getCell(j));
                    }
                    lignes.add(cases);
                }
            }
            return lignes;
        } catch (IOException | RuntimeException e) {
            // RuntimeException : POI signale un faux .xlsx par des exceptions non
            // vérifiées ; sans ce catch, un mauvais fichier donnerait un 500.
            throw new FichierIllisibleException();
        }
    }

    // Compte existant réutilisé tel quel ; sinon compte MEMBRE créé sans mot de passe
    // (colonne null) : personne ne peut s'y connecter tant que le membre ne l'a pas choisi.
    // Choix expliqué dans le journal des décisions du README.
    private Utilisateur trouverOuCreerMembre(String[] ligne) {
        Optional<Utilisateur> existant = utilisateurRepository.findByTelephone(ligne[2]);
        if (existant.isPresent()) {
            return existant.get();
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(ligne[0]);
        utilisateur.setPrenom(ligne[1]);
        utilisateur.setTelephone(ligne[2]);
        utilisateur.setRole(RoleUtilisateur.MEMBRE);

        // Email optionnel. Déjà pris par un autre compte : exception métier, rattrapée par
        // la boucle d'import, la ligne part dans le rapport (au lieu d'un 500 sur l'unicité).
        if (ligne.length > 3 && !ligne[3].isBlank()) {
            if (utilisateurRepository.existsByEmail(ligne[3])) {
                throw new EmailDejaUtiliseException(ligne[3]);
            }
            utilisateur.setEmail(ligne[3]);
        }

        Utilisateur enregistre = utilisateurRepository.save(utilisateur);
        return enregistre;
    }

    // Renvoie le motif de refus d'une ligne, ou null si elle est valide.
    // La taille est testée en premier : lire ligne[2] sur une ligne de 2 cases planterait.
    // Téléphone : 9 chiffres commençant par 7 (mobile sénégalais).
    private String verifierLigne(String[] ligne) {
        if (ligne.length < 3) {
            return "colonnes manquantes (nom, prenom, telephone attendus)";
        }
        if (ligne[0].isBlank()) {
            return "nom manquant";
        }
        if (ligne[1].isBlank()) {
            return "prenom manquant";
        }
        if (!ligne[2].matches("7\\d{8}")) {
            return "telephone invalide (9 chiffres commençant par 7)";
        }
        if (ligne.length > 4 && !ligne[4].isBlank() && !ligne[4].matches("[1-9]\\d*")) {
            return "nombre de parts invalide (entier à partir de 1)";
        }
        if (ligne.length > 3
                && !ligne[3].isBlank()
                && !ligne[3].matches(".+@.+\\..+")) {
            return "email invalide";
        }

        return null;
    }

    // Vide = aucune case ne contient de texte. Une seule règle pour les trois
    // formes possibles : tableau sans case, [""] et ["", "", ""].
    private boolean estVide(String[] ligne) {
        for (String valeur : ligne) {
            if (!valeur.isBlank()) {
                return false;
            }
        }
        return true;
    }

    // Colonne parts optionnelle : absente ou vide = 1 part. parseInt est sûr ici,
    // verifierLigne a déjà refusé tout ce qui ne ressemble pas à un entier ≥ 1.
    private int lireParts(String[] ligne) {
        if (ligne.length < 5 || ligne[4].isBlank()) {
            return 1;
        } else {
            return Integer.parseInt(ligne[4]);
        }
    }

    private ImportMembreDTO convertiImportMembreDTO(ImportMembre importMembre) {
        ImportMembreDTO dto = new ImportMembreDTO();
        dto.setId(importMembre.getId());
        dto.setTontineId(importMembre.getTontine().getId());
        dto.setFichierNom(importMembre.getFichierNom());
        dto.setNbMembresTotal(importMembre.getNbMembresTotal());
        dto.setNbImportes(importMembre.getNbImportes());
        dto.setNbErreurs(importMembre.getNbErreurs());
        dto.setErreursDetail(importMembre.getErreursDetail());
        dto.setStatut(importMembre.getStatut());
        dto.setCreatedAt(importMembre.getCreatedAt());

        return dto;
    }
}
