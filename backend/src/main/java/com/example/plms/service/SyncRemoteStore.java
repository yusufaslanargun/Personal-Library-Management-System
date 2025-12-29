package com.example.plms.service;

import com.example.plms.web.dto.sync.SyncPayload;
import java.util.Map;

public record SyncRemoteStore(Map<String, SyncPayload> clients) {
}
