package com.samanatteu.controller.pret;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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

}
