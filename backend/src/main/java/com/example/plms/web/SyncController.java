package com.example.plms.web;

import com.example.plms.service.SyncService;
import com.example.plms.web.dto.SyncEnableRequest;
import com.example.plms.web.dto.SyncStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
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
}
