package com.revature.vault.controller;

import com.revature.vault.dto.FolderDTO;
import com.revature.vault.dto.response.MessageResponse;
import com.revature.vault.service.vault.FolderService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.revature.vault.dto.response.VaultEntryResponse;
import com.revature.vault.service.vault.VaultService;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

  private final FolderService folderService;
  private final VaultService vaultService;

  @GetMapping
  public ResponseEntity<List<FolderDTO>> getFolders() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.getFolders(username));
  }

  @GetMapping("/{id}")
  public ResponseEntity<FolderDTO> getFolderById(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.getFolderById(id, username));
  }

  @PostMapping
  public ResponseEntity<FolderDTO> createFolder(
      @RequestParam String name,
      @RequestParam(required = false) Long parentFolderId) {
    String username = getCurrentUsername();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(folderService.createFolder(name, parentFolderId, username));
  }

  @PutMapping("/{id}")
  public ResponseEntity<FolderDTO> updateFolder(
      @PathVariable Long id,
      @RequestParam String name) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.updateFolder(id, name, username));
  }

  @PutMapping("/{id}/move")
  public ResponseEntity<FolderDTO> moveFolder(
      @PathVariable Long id,
      @RequestParam(required = false) Long parentId) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.moveFolder(id, parentId, username));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<MessageResponse> deleteFolder(@PathVariable Long id) {
    String username = getCurrentUsername();
    folderService.deleteFolder(id, username);
    return ResponseEntity.ok(new MessageResponse("Folder deleted successfully"));
  }

  @GetMapping("/{id}/entries")
  public ResponseEntity<List<VaultEntryResponse>> getEntriesInFolder(
      @PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getEntriesByFolder(username, id));
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}

