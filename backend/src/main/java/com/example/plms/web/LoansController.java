package com.example.plms.web;

import com.example.plms.service.LoanService;
import com.example.plms.security.AuthenticatedUser;
import com.example.plms.web.dto.LoanRequest;
import com.example.plms.web.dto.LoanResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public LoanResponse loan(@AuthenticationPrincipal AuthenticatedUser user,
                             @PathVariable Long id, @Valid @RequestBody LoanRequest request) {
        return loanService.createLoan(user.id(), id, request);
    }

    @GetMapping("/items/{id}/loan")
    public LoanResponse activeLoan(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return loanService.activeLoan(user.id(), id);
    }

    @PostMapping("/loans/{loanId}/return")
    public LoanResponse returnLoan(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long loanId) {
        return loanService.returnLoan(user.id(), loanId);
    }

    @GetMapping("/loans/overdue")
    public List<LoanResponse> overdue(@AuthenticationPrincipal AuthenticatedUser user) {
        return loanService.overdue(user.id());
    }
}
