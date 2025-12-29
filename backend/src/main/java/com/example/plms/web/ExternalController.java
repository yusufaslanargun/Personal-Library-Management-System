package com.example.plms.web;

import com.example.plms.external.ExternalBookCandidate;
import com.example.plms.service.ExternalIntegrationService;
import com.example.plms.security.AuthenticatedUser;
import com.example.plms.web.dto.ExternalApplyRequest;
import com.example.plms.web.dto.ExternalDiffField;
import com.example.plms.web.dto.ExternalLinkRequest;
import com.example.plms.web.dto.IsbnConfirmRequest;
import com.example.plms.web.dto.ItemResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ExternalController {
    private final ExternalIntegrationService externalService;

    public ExternalController(ExternalIntegrationService externalService) {
        this.externalService = externalService;
    }

    @GetMapping("/external/books/lookup")
    public List<ExternalBookCandidate> lookup(@RequestParam String isbn) {
        return externalService.lookupIsbn(isbn);
    }

    @PostMapping("/external/books/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse confirm(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody IsbnConfirmRequest request) {
        return externalService.createFromIsbn(user.id(), request);
    }

    @PostMapping("/items/{id}/external-link")
    public ItemResponse attachLink(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long id, @RequestBody ExternalLinkRequest request) {
        return externalService.attachExternalLink(user.id(), id, request);
    }

    @GetMapping("/items/{id}/external-refresh")
    public List<ExternalDiffField> refresh(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return externalService.refreshExternal(user.id(), id);
    }

    @PostMapping("/items/{id}/external-apply")
    public ItemResponse apply(@AuthenticationPrincipal AuthenticatedUser user,
                              @PathVariable Long id, @RequestBody ExternalApplyRequest request) {
        return externalService.applyExternal(user.id(), id, request);
    }
}
