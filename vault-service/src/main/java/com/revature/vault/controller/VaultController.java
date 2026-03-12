package com.revature.vault.controller;

import com.revature.vault.dto.request.VaultEntryRequest;
import com.revature.vault.dto.response.SnapshotResponse;
import com.revature.vault.dto.response.TrashEntryResponse;
import com.revature.vault.dto.response.MessageResponse;
import com.revature.vault.dto.response.VaultEntryDetailResponse;
import com.revature.vault.dto.response.VaultEntryResponse;
import com.revature.vault.service.vault.VaultService;
import com.revature.vault.service.vault.VaultSnapshotService;
import com.revature.vault.service.vault.VaultTrashService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.revature.vault.dto.request.SensitiveAccessRequest;
import com.revature.vault.dto.response.ViewPasswordResponse;
import com.revature.vault.dto.response.TrashCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
@Tag(name = "Vault", description = "Endpoints for managing passwords, notes, and secure data")
public class VaultController {

  private final VaultService vaultService;
  private final VaultTrashService vaultTrashService;
  private final VaultSnapshotService vaultSnapshotService;

  @Operation(summary = "Create a new entry in the user's vault")
  @PostMapping
  public ResponseEntity<VaultEntryResponse> createEntry(@RequestBody VaultEntryRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(vaultService.createEntry(username, request));
  }

  @Operation(summary = "Retrieve all entries in the user's vault")
  @GetMapping
  public ResponseEntity<List<VaultEntryResponse>> getAllEntries() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getAllEntries(username));
  }

  @Operation(summary = "Search for vault entries via various filters")
  @GetMapping("/search")
  public ResponseEntity<List<VaultEntryResponse>> searchEntries(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long folderId,
      @RequestParam(required = false) Boolean isFavorite,
      @RequestParam(required = false) Boolean isHighlySensitive,
      @RequestParam(defaultValue = "title") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.searchEntries(
        username, keyword, categoryId, folderId, isFavorite, isHighlySensitive, sortBy, sortDir));
  }

  @Operation(summary = "Filter vault entries by category or folder")
  @GetMapping("/filter")
  public ResponseEntity<List<VaultEntryResponse>> filterEntries(
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long folderId,
      @RequestParam(required = false) Boolean isFavorite) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.searchEntries(
        username, null, categoryId, folderId, isFavorite, null, "title", "asc"));
  }

  @Operation(summary = "Fetch highly recent vault entries")
  @GetMapping("/recent")
  public ResponseEntity<List<VaultEntryResponse>> getRecentEntries() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getRecentEntries(username));
  }

  @Operation(summary = "Fetch previously used vault entries")
  @GetMapping("/recently-used")
  public ResponseEntity<List<VaultEntryResponse>> getRecentlyUsedEntries() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getRecentlyUsedEntries(username));
  }

  @Operation(summary = "Fetch a specific vault entry by its ID")
  @GetMapping("/{id}")
  public ResponseEntity<VaultEntryDetailResponse> getEntry(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getEntry(username, id));
  }

  @Operation(summary = "Modify an existing vault entry")
  @PutMapping("/{id}")
  public ResponseEntity<VaultEntryResponse> updateEntry(
      @PathVariable Long id,
      @RequestBody VaultEntryRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.updateEntry(username, id, request));
  }

  @Operation(summary = "Toggle the favorite status of an entry")
  @PutMapping("/{id}/favorite")
  public ResponseEntity<VaultEntryResponse> toggleFavorite(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.toggleFavorite(username, id));
  }

  @Operation(summary = "Fetch all favorited vault entries")
  @GetMapping("/favorites")
  public ResponseEntity<List<VaultEntryResponse>> getFavorites() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getFavorites(username));
  }

  @Operation(summary = "Move multiple vault entries to the trash")
  @PostMapping("/entries/bulk-delete")
  public ResponseEntity<MessageResponse> bulkDelete(@RequestBody List<Long> ids) {
    String username = getCurrentUsername();
    vaultService.bulkDelete(username, ids);
    return ResponseEntity.ok(new MessageResponse("Entries deleted successfully"));
  }

  @Operation(summary = "Reveal plain text password for a specific entry")
  @PostMapping("/entries/{id}/view-password")
  public ResponseEntity<ViewPasswordResponse> viewPassword(@PathVariable Long id) {
    String username = getCurrentUsername();
    String password = vaultService.getPassword(username, id);
    return ResponseEntity.ok(new ViewPasswordResponse(password));
  }

  @Operation(summary = "Move a specific entry to the trash")
  @DeleteMapping("/{id}")
  public ResponseEntity<MessageResponse> deleteEntry(@PathVariable Long id) {
    String username = getCurrentUsername();
    vaultService.deleteEntry(username, id);
    return ResponseEntity.ok(new MessageResponse("Entry deleted successfully"));
  }

  @Operation(summary = "Toggle whether an entry requires master password to view")
  @PutMapping("/entries/{id}/sensitive")
  public ResponseEntity<VaultEntryResponse> toggleSensitive(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.toggleSensitive(username, id));
  }

  @Operation(summary = "Request access to view a highly sensitive vault entry")
  @PostMapping("/{id}/sensitive-view")
  public ResponseEntity<VaultEntryDetailResponse> accessSensitiveEntry(
      @PathVariable Long id,
      @RequestBody @jakarta.validation.Valid SensitiveAccessRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.accessSensitiveEntry(username, id, request));
  }

  @Operation(summary = "View historic snapshots of an entry's lifecycle")
  @GetMapping("/entries/{id}/history")
  public ResponseEntity<List<SnapshotResponse>> getPasswordHistory(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultSnapshotService.getHistory(username, id));
  }

  @Operation(summary = "List all entries currently in the trash")
  @GetMapping("/trash")
  public ResponseEntity<List<TrashEntryResponse>> getTrashEntries() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultTrashService.getTrashEntries(username));
  }

  @Operation(summary = "Get the total count of items in the trash")
  @GetMapping("/trash/count")
  public ResponseEntity<TrashCountResponse> getTrashCount() {
    String username = getCurrentUsername();
    long count = vaultTrashService.getTrashCount(username);
    return ResponseEntity.ok(new TrashCountResponse(count));
  }

  @Operation(summary = "Restore a specific entry from the trash to the main vault")
  @PostMapping("/trash/{id}/restore")
  public ResponseEntity<TrashEntryResponse> restoreEntry(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultTrashService.restoreEntry(username, id));
  }

  @Operation(summary = "Restore all entries currently in the trash bin")
  @PostMapping("/trash/restore-all")
  public ResponseEntity<MessageResponse> restoreAll() {
    String username = getCurrentUsername();
    vaultTrashService.restoreAll(username);
    return ResponseEntity.ok(new MessageResponse("All trash entries restored successfully"));
  }

  @Operation(summary = "Irreversibly delete a specific vault entry")
  @DeleteMapping("/trash/{id}")
  public ResponseEntity<MessageResponse> permanentDelete(@PathVariable Long id) {
    String username = getCurrentUsername();
    vaultTrashService.permanentDelete(username, id);
    return ResponseEntity.ok(new MessageResponse("Entry permanently deleted"));
  }

  @Operation(summary = "Irreversibly delete all items inside the trash")
  @DeleteMapping("/trash/empty")
  public ResponseEntity<MessageResponse> emptyTrash() {
    String username = getCurrentUsername();
    vaultTrashService.emptyTrash(username);
    return ResponseEntity.ok(new MessageResponse("Trash emptied successfully"));
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}

