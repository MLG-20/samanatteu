package com.samanatteu.service.pret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.samanatteu.entity.pret.Transaction;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.ModePaiement;
import com.samanatteu.enums.pret.SensTransaction;
import com.samanatteu.enums.pret.TypeTransaction;

// Tests de la fabrication du reçu PDF.
//
// L'astuce : PDFBox sait RELIRE un PDF. On génère le reçu, on le rouvre, on en
// extrait le texte, et on vérifie ce qu'il contient. Le test porte donc sur le
// contenu réellement affiché au membre, pas sur des détails internes.
class RecuPDFServiceTest {

    private final RecuPDFService service = new RecuPDFService();

    private Transaction transaction(String nomTontine, String prenom, String nom, String montant,
            ModePaiement mode, String reference, String description) {
        Utilisateur membre = new Utilisateur();
        membre.setPrenom(prenom);
        membre.setNom(nom);
        Tontine tontine = new Tontine();
        tontine.setNom(nomTontine);

        Transaction transaction = new Transaction();
        transaction.setMembre(membre);
        transaction.setTontine(tontine);
        transaction.setType(TypeTransaction.COTISATION);
        transaction.setSens(SensTransaction.ENTRANT);
        transaction.setMontant(new BigDecimal(montant));
        transaction.setModePaiement(mode);
        transaction.setReference(reference);
        transaction.setDescription(description);
        transaction.setDateTransaction(LocalDateTime.of(2026, 10, 20, 15, 30));
        return transaction;
    }

    // Le texte du PDF généré.
    private String texteDuPdf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    // Règle : le reçu contient les informations du paiement.
    @Test
    void leRecu_contientLesInfosDuPaiement() throws IOException {
        Transaction transaction = transaction("Natt des femmes", "Awa", "Diop", "15000",
                ModePaiement.WAVE, "WAVE-123", "Paiement de la cotisation du cycle 1");

        String texte = texteDuPdf(service.genererRecu(transaction));

        assertTrue(texte.contains("Natt des femmes"), "la tontine doit etre dans le recu");
        assertTrue(texte.contains("Awa Diop"), "le membre doit etre dans le recu");
        assertTrue(texte.contains("15000 FCFA"), "le montant doit etre dans le recu");
        assertTrue(texte.contains("WAVE"), "le mode de paiement doit etre dans le recu");
        assertTrue(texte.contains("WAVE-123"), "la reference doit etre dans le recu");
        assertTrue(texte.contains("20/10/2026 15:30"), "la date doit etre dans le recu");
    }

    // Règle : les centimes inutiles sont retirés (15000, pas 15000.00).
    @Test
    void leRecu_nEcritPasLesCentimes() throws IOException {
        Transaction transaction = transaction("Tontine", "Awa", "Diop", "15000.00",
                ModePaiement.CASH, "REF-1", "Description");

        String texte = texteDuPdf(service.genererRecu(transaction));

        assertTrue(texte.contains("15000 FCFA"), "le montant doit etre sans centimes : " + texte);
        assertTrue(!texte.contains("15000.00"), "les centimes ne doivent pas apparaitre : " + texte);
    }

    // Règle : un mode de paiement ou une reference absents ne font pas planter
    // le recu (le journal des gains et des prêts ne les remplit pas).
    @Test
    void leRecu_gereLesInformationsAbsentes() throws IOException {
        Transaction transaction = transaction("Tontine", "Awa", "Diop", "5000",
                null, null, "Description");

        String texte = texteDuPdf(service.genererRecu(transaction));

        assertTrue(texte.contains("non precise"), "une info absente doit etre signalee : " + texte);
    }

    // Règle : une description absente ne doit pas s'afficher "null" (le
    // concaténation "Description : " + null donne "Description : null").
    @Test
    void leRecu_nAffichePasLeMotNull() throws IOException {
        Transaction transaction = transaction("Tontine", "Awa", "Diop", "5000",
                ModePaiement.CASH, "REF-1", null);

        String texte = texteDuPdf(service.genererRecu(transaction));

        assertTrue(!texte.contains("null"), "le recu ne doit pas contenir 'null' : " + texte);
        assertTrue(texte.contains("non precise"), "l'info absente doit etre signalee : " + texte);
    }

    // Règle : un nom avec un caractère hors Latin-1 ne doit pas faire planter la
    // generation (le police standard ne sait pas l'ecrire).
    @Test
    void leRecu_nePlantePasSurUnCaractereExotique() throws IOException {
        Transaction transaction = transaction("Tontine", "开始", "Diop", "5000",
                ModePaiement.CASH, "REF-1", "Description");

        String texte = texteDuPdf(service.genererRecu(transaction));

        // Le caractere est remplace, pas perdu : le recu existe et contient le membre.
        assertTrue(texte.contains("Diop"), "le recu doit etre genere quand meme : " + texte);
    }

    // Règle : le PDF produit est un vrai PDF (signature %PDF en tete de fichier).
    @Test
    void leRecu_produitUnVraiPdf() {
        Transaction transaction = transaction("Tontine", "Awa", "Diop", "5000",
                ModePaiement.CASH, "REF-1", "Description");

        byte[] pdf = service.genererRecu(transaction);

        assertTrue(pdf.length > 0, "le PDF ne doit pas etre vide");
        assertEquals('%', (char) pdf[0], "le fichier doit commencer par %PDF");
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }
}
