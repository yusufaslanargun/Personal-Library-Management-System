package com.example.plms.web;

import com.example.plms.service.LoanService;
import com.example.plms.web.dto.LoanRequest;
import com.example.plms.web.dto.LoanResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class LoansController {
    private final LoanService loanService;

    public LoansController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/items/{id}/loan")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse loan(@PathVariable Long id, @Valid @RequestBody LoanRequest request) {
        return loanService.createLoan(id, request);
    }

    @GetMapping("/items/{id}/loan")
    public LoanResponse activeLoan(@PathVariable Long id) {
        return loanService.activeLoan(id);
    }

    @PostMapping("/loans/{loanId}/return")
    public LoanResponse returnLoan(@PathVariable Long loanId) {
        return loanService.returnLoan(loanId);
    }

    @GetMapping("/loans/overdue")
    public List<LoanResponse> overdue() {
        return loanService.overdue();
    }
}
