package com.revature.vault.controller;

import com.revature.vault.dto.request.ImportRequest;
import com.revature.vault.dto.request.ThirdPartyImportRequest;
import com.revature.vault.dto.response.ExportResponse;
import com.revature.vault.dto.response.ImportResult;
import com.revature.vault.service.backup.ExportService;
import com.revature.vault.service.backup.ImportService;
import com.revature.vault.service.backup.ThirdPartyImportService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

  private static final Logger logger = LoggerFactory.getLogger(BackupController.class);

  private final ExportService exportService;
  private final ImportService importService;
  private final ThirdPartyImportService thirdPartyImportService;
  private final com.revature.vault.service.vault.VaultSnapshotService vaultSnapshotService;

  @GetMapping("/export")
  public ResponseEntity<ExportResponse> exportVault(@RequestHeader("X-User-Name") String username,      @RequestParam(defaultValue = "JSON") String format,
      @RequestParam(required = false) String password) {
    return ResponseEntity.ok(exportService.exportVault(username, format, password));
  }

  @PostMapping("/import")
  public ResponseEntity<ImportResult> importVault(@RequestHeader("X-User-Name") String username, @RequestBody ImportRequest request) {
    logger.info("Import request received in controller");
    logger.debug("Request format: {}, data length: {}",
        request.getFormat(),
        request.getData() != null ? request.getData().length() : 0);
    return ResponseEntity.ok(importService.importVault(username, request));
  }

  @PostMapping("/import-external")
  public ResponseEntity<ImportResult> importFromExternal(@RequestHeader("X-User-Name") String username, @RequestBody ThirdPartyImportRequest request) {
    return ResponseEntity.ok(thirdPartyImportService.importFromThirdParty(username, request));
  }

  @GetMapping("/export/preview")
  public ResponseEntity<ExportResponse> previewExport(@RequestHeader("X-User-Name") String username, @RequestParam(defaultValue = "JSON") String format) {
    return ResponseEntity.ok(exportService.previewExport(username, format));
  }

  @PostMapping("/import/validate")
  public ResponseEntity<ImportResult> validateImport(@RequestHeader("X-User-Name") String username, @RequestBody ImportRequest request) {
    return ResponseEntity.ok(importService.validateImport(username, request));
  }

  @GetMapping("/import-external/formats")
  public ResponseEntity<java.util.List<String>> getSupportedFormats() {
    return ResponseEntity.ok(thirdPartyImportService.getSupportedFormats());
  }

  @GetMapping("/snapshots")
  public ResponseEntity<java.util.List<com.revature.vault.dto.response.SnapshotResponse>> getAllSnapshots(@RequestHeader("X-User-Name") String username) {
    return ResponseEntity.ok(vaultSnapshotService.getAllSnapshots(username));
  }

  @PostMapping("/snapshots/{id}/restore")
  public ResponseEntity<Void> restoreSnapshot(@RequestHeader("X-User-Name") String username, @PathVariable Long id) {
    vaultSnapshotService.restoreSnapshot(username, id);
    return ResponseEntity.ok().build();
  }

  
}







