package com.samanatteu.controller.pret;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.pret.TransactionDTO;
import com.samanatteu.service.pret.TransactionService;

@RequestMapping("/transaction")
@RestController
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionDTO> listTransactions() {
        return transactionService.listTransactions();
    }

    // Le reçu PDF d'un paiement.
    // Le PDF est renvoyé tel quel (application/pdf) avec un Content-Disposition
    // "attachment" : le navigateur le TÉLÉCHARGE au lieu de l'afficher.
    // Sans cet en-tête, le navigateur essaierait d'afficher le PDF dans la page.
    @GetMapping("/{id}/recu")
    public ResponseEntity<byte[]> recu(@PathVariable Long id) {
        byte[] pdf = transactionService.genererRecu(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recu-" + id + ".pdf\"")
                .body(pdf);
    }

}
