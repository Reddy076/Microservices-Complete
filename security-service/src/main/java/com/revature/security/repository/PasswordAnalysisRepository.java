package com.revature.security.repository;

import com.revature.security.model.PasswordAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordAnalysisRepository extends JpaRepository<PasswordAnalysis, Long> {
  Optional<PasswordAnalysis> findByVaultEntryId(Long vaultEntryId);

  void deleteByVaultEntryId(Long vaultEntryId);

  List<PasswordAnalysis> findByVaultEntryIdIn(List<Long> vaultEntryIds);
}
