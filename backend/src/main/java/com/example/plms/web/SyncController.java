package com.example.plms.web;

import com.example.plms.service.SyncRemoteService;
import com.example.plms.service.SyncService;
import com.example.plms.security.AuthenticatedUser;
import com.example.plms.web.dto.SyncEnableRequest;
import com.example.plms.web.dto.SyncStatusResponse;
import com.example.plms.web.dto.sync.SyncRequest;
import com.example.plms.web.dto.sync.SyncResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private final SyncService syncService;
    private final SyncRemoteService syncRemoteService;

    public SyncController(SyncService syncService, SyncRemoteService syncRemoteService) {
        this.syncService = syncService;
        this.syncRemoteService = syncRemoteService;
    }

    @PostMapping("/enable")
    public SyncStatusResponse enable(@AuthenticationPrincipal AuthenticatedUser user,
                                     @RequestBody SyncEnableRequest request) {
        return syncService.enable(user.id(), request.enabled());
    }

    @PostMapping("/run")
    public SyncStatusResponse run(@AuthenticationPrincipal AuthenticatedUser user) {
        return syncService.runSync(user.id());
    }

    @GetMapping("/status")
    public SyncStatusResponse status(@AuthenticationPrincipal AuthenticatedUser user) {
        return syncService.status(user.id());
    }

    @PostMapping("/remote")
    public SyncResponse remote(@RequestHeader(name = "X-API-Key", required = false) String apiKey,
                               @RequestBody SyncRequest request) {
        return syncRemoteService.handle(request, apiKey);
    }
}
