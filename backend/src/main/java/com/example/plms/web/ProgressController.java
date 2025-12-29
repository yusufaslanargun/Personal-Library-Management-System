package com.example.plms.web;

import com.example.plms.service.ProgressService;
import com.example.plms.security.AuthenticatedUser;
import com.example.plms.web.dto.ProgressLogRequest;
import com.example.plms.web.dto.ProgressLogResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items/{id}/progress")
public class ProgressController {
    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgressLogResponse log(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long id, @Valid @RequestBody ProgressLogRequest request) {
        return progressService.logProgress(user.id(), id, request);
    }

    @GetMapping
    public List<ProgressLogResponse> history(@AuthenticationPrincipal AuthenticatedUser user,
                                             @PathVariable Long id) {
        return progressService.history(user.id(), id);
    }

    @DeleteMapping("/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable Long id,
                       @PathVariable Long logId) {
        progressService.deleteLog(user.id(), id, logId);
    }
}
