package com.revature.vault.controller;

import com.revature.vault.dto.CategoryDTO;
import com.revature.vault.dto.request.CreateCategoryRequest;
import com.revature.vault.dto.response.MessageResponse;
import com.revature.vault.service.vault.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.revature.vault.dto.response.VaultEntryResponse;
import com.revature.vault.service.vault.VaultService;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;
  private final VaultService vaultService;

  @GetMapping
  public ResponseEntity<List<CategoryDTO>> getAllCategories() {
    String username = getCurrentUsername();
    List<CategoryDTO> categories = categoryService.getAllCategories(username);
    return ResponseEntity.ok(categories);
  }

  @GetMapping("/{id}")
  public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
    String username = getCurrentUsername();
    CategoryDTO category = categoryService.getCategoryById(id, username);
    return ResponseEntity.ok(category);
  }

  @PostMapping
  public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
    String username = getCurrentUsername();
    CategoryDTO category = categoryService.createCategory(request, username);
    return ResponseEntity.status(HttpStatus.CREATED).body(category);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CategoryDTO> updateCategory(
      @PathVariable Long id,
      @Valid @RequestBody CreateCategoryRequest request) {
    String username = getCurrentUsername();
    CategoryDTO category = categoryService.updateCategory(id, request, username);
    return ResponseEntity.ok(category);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Long id) {
    String username = getCurrentUsername();
    categoryService.deleteCategory(id, username);
    return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
  }

  @GetMapping("/{id}/entries")
  public ResponseEntity<List<VaultEntryResponse>> getEntriesInCategory(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getEntriesByCategory(username, id));
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}

