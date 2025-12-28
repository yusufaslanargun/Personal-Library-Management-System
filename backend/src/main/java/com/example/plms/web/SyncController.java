package com.example.plms.web;

import com.example.plms.service.SyncRemoteService;
import com.example.plms.service.SyncService;
import com.example.plms.web.dto.SyncEnableRequest;
import com.example.plms.web.dto.SyncStatusResponse;
import com.example.plms.web.dto.sync.SyncRequest;
import com.example.plms.web.dto.sync.SyncResponse;
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
    public SyncStatusResponse enable(@RequestBody SyncEnableRequest request) {
        return syncService.enable(request.enabled());
    }

    @PostMapping("/run")
    public SyncStatusResponse run() {
        return syncService.runSync();
    }

    @GetMapping("/status")
    public SyncStatusResponse status() {
        return syncService.status();
    }

    @PostMapping("/remote")
    public SyncResponse remote(@RequestHeader(name = "X-API-Key", required = false) String apiKey,
                               @RequestBody SyncRequest request) {
        return syncRemoteService.handle(request, apiKey);
    }
}
