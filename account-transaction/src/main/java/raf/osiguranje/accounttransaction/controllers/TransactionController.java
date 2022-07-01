package raf.osiguranje.accounttransaction.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import raf.osiguranje.accounttransaction.model.Transaction;
import raf.osiguranje.accounttransaction.model.dto.TransactionDTO;
import raf.osiguranje.accounttransaction.services.TransactionService;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    private TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionDTO input){

        try {
            transactionService.createTransaction(input);
            return ResponseEntity.accepted().build();
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(path="/all")
    public ResponseEntity<List<TransactionDTO>> findAllTransactions(){
        List<Transaction> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions.stream().map(Transaction::getDto).collect(Collectors.toList()));
    }

    @GetMapping(path="/account")
    public ResponseEntity<List<TransactionDTO>> findAllTransactionsByAccount(@RequestParam("account") Long input){
        List<Transaction> transactions = transactionService.getTransactionsByAccount(input);
        return ResponseEntity.ok(transactions.stream().map(Transaction::getDto).collect(Collectors.toList()));
    }

    @GetMapping(path="/currency")
    public ResponseEntity<List<TransactionDTO>> findAllTransactionsByForex(@RequestParam("currency") Long input){
        List<Transaction> transactions = transactionService.getTransactionsByCurrency(input);
        return ResponseEntity.ok(transactions.stream().map(Transaction::getDto).collect(Collectors.toList()));
    }

    @GetMapping(path="/user")
    public ResponseEntity<List<TransactionDTO>> findAllTransactionsByUser(@RequestParam("user") Long input){
        List<Transaction> transactions = transactionService.getTransactionsByUser(input);
        return ResponseEntity.ok(transactions.stream().map(Transaction::getDto).collect(Collectors.toList()));
    }

    @GetMapping(path="/order")
    public ResponseEntity<List<TransactionDTO>> findAllTransactionsByOrder(@RequestParam("order") Long input){
        List<Transaction> transactions = transactionService.getTransactionsByOrderId(input);
        return ResponseEntity.ok(transactions.stream().map(Transaction::getDto).collect(Collectors.toList()));
    }

}
