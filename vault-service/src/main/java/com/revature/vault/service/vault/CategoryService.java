package com.revature.vault.service.vault;

import com.revature.vault.dto.CategoryDTO;
import com.revature.vault.dto.request.CreateCategoryRequest;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.model.vault.Category;
import com.revature.vault.repository.CategoryRepository;
import com.revature.vault.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final UserClient userClient;

  public List<CategoryDTO> getAllCategories(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<Category> userCategories = categoryRepository.findByUserIdOrderByNameAsc(user.getId());

    List<Category> defaultCategories = categoryRepository.findByUserIdIsNullAndIsDefaultTrue();

    List<CategoryDTO> allCategories = defaultCategories.stream()
        .map(this::toDTO)
        .collect(Collectors.toList());

    allCategories.addAll(userCategories.stream()
        .map(this::toDTO)
        .collect(Collectors.toList()));

    return allCategories;
  }

  public CategoryDTO getCategoryById(Long categoryId, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
        .orElseGet(() -> categoryRepository.findById(categoryId)
            .filter(c -> Boolean.TRUE.equals(c.getIsDefault()) && c.getUserId() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId)));

    return toDTO(category);
  }

  @Transactional
  public CategoryDTO createCategory(CreateCategoryRequest request, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    if (categoryRepository.existsByUserIdAndName(user.getId(), request.getName())) {
      throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
    }

    Category category = Category.builder()
        .userId(user.getId())
        .name(request.getName())
        .icon(request.getIcon())
        .isDefault(false)
        .build();

    Category savedCategory = categoryRepository.save(category);
    return toDTO(savedCategory);
  }

  @Transactional
  public CategoryDTO updateCategory(Long categoryId, CreateCategoryRequest request, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

    if (Boolean.TRUE.equals(category.getIsDefault())) {
      throw new IllegalArgumentException("Cannot modify default categories");
    }

    categoryRepository.findByUserIdAndName(user.getId(), request.getName())
        .ifPresent(existing -> {
          if (!existing.getId().equals(categoryId)) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
          }
        });

    category.setName(request.getName());
    category.setIcon(request.getIcon());

    Category updatedCategory = categoryRepository.save(category);
    return toDTO(updatedCategory);
  }

  @Transactional
  public void deleteCategory(Long categoryId, String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

    if (Boolean.TRUE.equals(category.getIsDefault())) {
      throw new IllegalArgumentException("Cannot delete default categories");
    }

    categoryRepository.delete(category);
  }

  public long getCategoryCount(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    return categoryRepository.countByUserId(user.getId());
  }

  private CategoryDTO toDTO(Category category) {
    return CategoryDTO.builder()
        .id(category.getId())
        .name(category.getName())
        .icon(category.getIcon())
        .isDefault(category.getIsDefault())
        .createdAt(category.getCreatedAt())
        .entryCount(0)
        .build();
  }
}


