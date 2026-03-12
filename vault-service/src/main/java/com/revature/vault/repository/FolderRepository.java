package com.revature.vault.repository;

import com.revature.vault.model.vault.Folder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
  List<Folder> findByUserIdAndParentFolderIsNull(Long userId);

  List<Folder> findByUserId(Long userId);

  Optional<Folder> findByIdAndUserId(Long id, Long userId);

  int countByUserId(Long userId);
}




