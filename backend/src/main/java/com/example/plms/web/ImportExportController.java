package com.example.plms.web;

import com.example.plms.service.ImportExportService;
import com.example.plms.web.dto.ExportBundle;
import com.example.plms.web.dto.ImportSummary;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ImportExportController {
    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportData(@RequestParam(defaultValue = "json") String format) {
        if ("csv".equalsIgnoreCase(format)) {
            byte[] zip = importExportService.exportCsvZip();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plms-export.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
        }
        ExportBundle bundle = importExportService.exportBundle();
        return ResponseEntity.ok(bundle);
    }

    @PostMapping("/import")
    public ResponseEntity<ImportSummary> importData(@RequestParam(defaultValue = "json") String format,
                                    @RequestParam("file") MultipartFile file) {
        try {
            byte[] payload = file.getBytes();
            if ("csv".equalsIgnoreCase(format)) {
                ImportSummary summary = importExportService.importCsv(payload);
                return buildImportResponse(summary);
            }
            ImportSummary summary = importExportService.importJson(payload);
            return buildImportResponse(summary);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read upload");
        }
    }

    private ResponseEntity<ImportSummary> buildImportResponse(ImportSummary summary) {
        if (summary.errors() != null && !summary.errors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(summary);
        }
        return ResponseEntity.ok(summary);
    }
}
