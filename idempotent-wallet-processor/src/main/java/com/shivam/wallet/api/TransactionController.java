package com.shivam.wallet.api;

import com.shivam.wallet.service.WalletTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final WalletTransactionService service;

    public TransactionController(WalletTransactionService service) {
        this.service = service;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> process(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(service.process(request));
    }
}
