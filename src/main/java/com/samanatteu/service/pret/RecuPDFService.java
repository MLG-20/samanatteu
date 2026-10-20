package com.samanatteu.service.pret;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.samanatteu.entity.pret.Transaction;

// Fabrique le reçu PDF d'un paiement.
//
// Service PUR : il ne lit pas la base et ne contrôle aucun droit (c'est le rôle
// de TransactionService). Il transforme une transaction en octets PDF. On peut
// donc le tester tout seul, sans Spring.
@Service
public class RecuPDFService {
    // Les polices standard 14 n'encodent que le Latin-1 (WinAnsi). Un nom avec
    // un caractère hors de ce jeu ferait planter showText() : on le remplace.
    private static final String REMPLACEMENT = "?";

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] genererRecu(Transaction transaction) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                contenu.beginText();

                contenu.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contenu.newLineAtOffset(50, 750);
                contenu.showText(nettoyer("SamaNatteu - Recu de paiement"));

                contenu.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                contenu.newLineAtOffset(0, -25);
                contenu.showText(nettoyer("Tontine : " + transaction.getTontine().getNom()));

                contenu.newLineAtOffset(0, -18);
                contenu.showText(nettoyer("Membre : " + transaction.getMembre().getPrenom()
                        + " " + transaction.getMembre().getNom()));

                contenu.newLineAtOffset(0, -18);
                contenu.showText(nettoyer("Montant : " + transaction.getMontant()
                        .stripTrailingZeros().toPlainString() + " FCFA"));

                contenu.newLineAtOffset(0, -18);
                contenu.showText(nettoyer("Mode de paiement : "
                        + (transaction.getModePaiement() == null ? "non precise"
                                : transaction.getModePaiement().name())));

                contenu.newLineAtOffset(0, -18);
                contenu.showText(nettoyer("Reference : "
                        + ouNonPrecise(transaction.getReference())));

                contenu.newLineAtOffset(0, -18);
                contenu.showText(nettoyer("Date : " + transaction.getDateTransaction().format(FORMAT_DATE)));

                contenu.newLineAtOffset(0, -18);
                contenu.showText(nettoyer("Description : "
                        + ouNonPrecise(transaction.getDescription())));

                contenu.newLineAtOffset(0, -30);
                contenu.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
                contenu.showText(nettoyer("Document genere par SamaNatteu - operation non modifiable."));

                contenu.endText();
            }

            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la generation du recu PDF", e);
        }
    }

    // Ne laisse passer que ce que la police sait ecrire : un caractere hors
    // Latin-1 ferait echouer showText() (et donc toute la generation).
    private String nettoyer(String texte) {
        if (texte == null) {
            return "";
        }
        StringBuilder resultat = new StringBuilder();
        for (char c : texte.toCharArray()) {
            resultat.append(c <= 0xFF ? c : REMPLACEMENT);
        }
        return resultat.toString();
    }

    // Une information absente est signalee, pas affichee comme "null".
    // Attention : le test doit porter sur la VALEUR, pas sur la concatenation
    // ("Description : " + null donne "Description : null").
    private String ouNonPrecise(String valeur) {
        return valeur == null ? "non precise" : valeur;
    }
}
