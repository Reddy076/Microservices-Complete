package com.revature.vault.repository;

import com.revature.vault.model.vault.Category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  List<Category> findByUserId(Long userId);

  List<Category> findByUserIdOrderByNameAsc(Long userId);

  Optional<Category> findByUserIdAndName(Long userId, String name);

  List<Category> findByUserIdIsNullAndIsDefaultTrue();

  Optional<Category> findByIdAndUserId(Long id, Long userId);

  boolean existsByUserIdAndName(Long userId, String name);

  long countByUserId(Long userId);

}

