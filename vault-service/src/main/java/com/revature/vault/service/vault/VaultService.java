package com.revature.vault.service.vault;

import com.revature.vault.dto.request.VaultEntryRequest;
import com.revature.vault.dto.response.VaultEntryDetailResponse;
import com.revature.vault.dto.response.VaultEntryResponse;
import com.revature.vault.exception.ResourceNotFoundException;
import com.revature.vault.client.UserClient;
import com.revature.vault.model.vault.Category;
import com.revature.vault.model.vault.Folder;
import com.revature.vault.model.vault.VaultEntry;
import com.revature.vault.repository.CategoryRepository;
import com.revature.vault.repository.FolderRepository;
import com.revature.vault.repository.SecureShareRepository;
import com.revature.vault.client.UserClient.UserVaultDetails;
import com.revature.vault.repository.VaultEntryRepository;
import com.revature.vault.client.SecurityClient.VaultEntryPayload;
import com.revature.vault.service.vault.EncryptionService;
import com.revature.vault.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.revature.vault.dto.request.SensitiveAccessRequest;
import com.revature.vault.exception.AuthenticationException;

import com.revature.vault.client.SecurityClient;
import com.revature.vault.client.NotificationClient;

@Service
@RequiredArgsConstructor
public class VaultService {

  private final VaultEntryRepository vaultEntryRepository;
  private final UserClient userClient;
  private final CategoryRepository categoryRepository;
  private final FolderRepository folderRepository;
  private final EncryptionService encryptionService;
  private final EncryptionUtil encryptionUtil;
  
  private final VaultSnapshotService vaultSnapshotService;
  private final SecurityClient securityClient;
  private final NotificationClient notificationClient;
  
  private final SecureShareRepository secureShareRepository;
  private final com.revature.vault.util.PasswordStrengthCalculator passwordStrengthCalculator;
  private final DuressService duressService;
  private final AuthenticationManager authenticationManager;

  private boolean isDuressMode() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null && attributes.getRequest() != null) {
      String isDuressHeader = attributes.getRequest().getHeader("X-Is-Duress");
      return "true".equalsIgnoreCase(isDuressHeader);
    }
    return false;
  }

  private void checkDuressMode() {
    if (isDuressMode()) {
      throw new AuthenticationException("Action not permitted in duress mode");
    }
  }

  @Transactional
  public VaultEntryResponse createEntry(String username, VaultEntryRequest request) {
    checkDuressMode();
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    checkReadOnlyMode(user);

    Category category = null;
    if (request.getCategoryId() != null) {
      category = categoryRepository.findById(request.getCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    Folder folder = null;
    if (request.getFolderId() != null) {
      folder = folderRepository.findById(request.getFolderId())
          .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
    }

    SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    String rawPassword = request.getPassword() != null ? request.getPassword() : "";
    String rawUsername = request.getUsername() != null ? request.getUsername() : "";

    String encryptedPassword = encryptionService.encrypt(rawPassword, key);
    String encryptedUsername = encryptionService.encrypt(rawUsername, key);
    String encryptedNotes = request.getNotes() != null ? encryptionService.encrypt(request.getNotes(), key) : null;

    VaultEntry entry = VaultEntry.builder()
        .userId(user.getId())
        .category(category)
        .folder(folder)
        .title(request.getTitle())
        .username(encryptedUsername)
        .password(encryptedPassword)
        .websiteUrl(request.getWebsiteUrl())
        .notes(encryptedNotes)
        .isFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : false)
        .isHighlySensitive(request.getIsHighlySensitive() != null ? request.getIsHighlySensitive() : false)
        .build();

    VaultEntry savedEntry = vaultEntryRepository.save(entry);

    securityClient.logAction(username, "ENTRY_CREATED", "Created entry: " + savedEntry.getTitle());

    notificationClient.createNotification(username, "ACCOUNT_ACTIVITY", "Vault Entry Created",
        "You created a new vault entry: '" + savedEntry.getTitle() + "'.");

    securityClient.analyzePasswordEntry(mapToPayload(savedEntry));

    return mapToResponse(savedEntry);
  }

  @Transactional(readOnly = true)
  public VaultEntryDetailResponse getEntry(String username, Long entryId) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    if (isDuressMode()) {
      VaultEntry dummy = duressService.generateDummyVault(user).stream()
          .filter(e -> e.getId().equals(entryId))
          .findFirst()
          .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));
      return mapToDetailResponse(dummy, dummy.getUsername(), "***encrypted***", "");
    }

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    String decryptedPassword;
    String decryptedUsername;
    String decryptedNotes;

    if (Boolean.TRUE.equals(entry.getIsHighlySensitive())) {
      @SuppressWarnings("java:S2068") // Not a hardcoded password, just a placeholder string
      String password = "******";
      decryptedPassword = password;
      decryptedUsername = "******";
      decryptedNotes = "******";
    } else {
      decryptedPassword = encryptionService.decrypt(entry.getPassword(), key);
      decryptedUsername = encryptionService.decrypt(entry.getUsername(), key);
      decryptedNotes = entry.getNotes() != null ? encryptionService.decrypt(entry.getNotes(), key) : null;
    }

    return mapToDetailResponse(entry, decryptedUsername, decryptedPassword, decryptedNotes);
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getAllEntries(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    if (isDuressMode()) {
      SecretKey key = getSharedKeySafely(user);
      return duressService.generateDummyVault(user).stream()
          .map(e -> mapToResponseWithKey(e, key))
          .collect(Collectors.toList());
    }

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());
    SecretKey key = getSharedKeySafely(user);
    return entries.stream()
        .map(e -> mapToResponseWithKey(e, key))
        .collect(Collectors.toList());
  }

  @Transactional
  public VaultEntryResponse updateEntry(String username, Long entryId, VaultEntryRequest request) {
    checkDuressMode();
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));
    checkReadOnlyMode(user);

    SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    if (request.getTitle() != null)
      entry.setTitle(request.getTitle());
    if (request.getWebsiteUrl() != null)
      entry.setWebsiteUrl(request.getWebsiteUrl());
    if (request.getIsFavorite() != null)
      entry.setIsFavorite(request.getIsFavorite());
    if (request.getIsHighlySensitive() != null)
      entry.setIsHighlySensitive(request.getIsHighlySensitive());

    if (request.getCategoryId() != null) {
      Category category = categoryRepository.findById(request.getCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
      entry.setCategory(category);
    }

    if (request.getFolderId() != null) {
      Folder folder = folderRepository.findById(request.getFolderId())
          .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
      entry.setFolder(folder);
    }

    if (request.getPassword() != null && !request.getPassword().equals("******")) {

      vaultSnapshotService.createSnapshot(entry);
      entry.setPassword(encryptionService.encrypt(request.getPassword(), key));

      // Gap 12 fix: auto-revoke all active shares for this entry since the
      // shared ciphertext now holds the old (stale) password.
      secureShareRepository.findActiveSharesByOwnerId(user.getId(), LocalDateTime.now())
          .stream()
          .filter(s -> s.getVaultEntry().getId().equals(entry.getId()))
          .forEach(s -> {
            s.setRevoked(true);
            secureShareRepository.save(s);
          });
    }
    if (request.getUsername() != null && !request.getUsername().equals("******")) {
      entry.setUsername(encryptionService.encrypt(request.getUsername(), key));
    }
    if (request.getNotes() != null && !request.getNotes().equals("******")) {
      entry.setNotes(encryptionService.encrypt(request.getNotes(), key));
    }

    VaultEntry savedEntry = vaultEntryRepository.save(entry);

    securityClient.logAction(username, "ENTRY_UPDATED", "Updated entry: " + savedEntry.getTitle());

    notificationClient.createNotification(username, "ACCOUNT_ACTIVITY", "Vault Entry Updated",
        "You updated the vault entry: '" + savedEntry.getTitle() + "'.");

    if (request.getPassword() != null) {
      securityClient.analyzePasswordEntry(mapToPayload(savedEntry));
    }

    return mapToResponse(savedEntry);
  }

  @Transactional
  public void deleteEntry(String username, Long entryId) {
    checkDuressMode();
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    checkReadOnlyMode(user);

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    entry.setIsDeleted(true);
    entry.setDeletedAt(LocalDateTime.now());
    vaultEntryRepository.save(entry);

    securityClient.logAction(username, "ENTRY_DELETED", "Deleted entry: " + entry.getTitle());

    notificationClient.createNotification(username, "ACCOUNT_ACTIVITY", "Vault Entry Deleted",
        "You moved the vault entry '" + entry.getTitle() + "' to the trash.");
  }

  @Transactional
  public VaultEntryResponse toggleFavorite(String username, Long entryId) {
    checkDuressMode();
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    checkReadOnlyMode(user);
    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    entry.setIsFavorite(!Boolean.TRUE.equals(entry.getIsFavorite()));
    VaultEntry savedEntry = vaultEntryRepository.save(entry);

    String actionNoun = Boolean.TRUE.equals(entry.getIsFavorite()) ? "Favorited" : "Unfavorited";
    String actionVerb = Boolean.TRUE.equals(entry.getIsFavorite()) ? "marked" : "removed";
    String preposition = Boolean.TRUE.equals(entry.getIsFavorite()) ? "as a favorite" : "from favorites";

    notificationClient.createNotification(username, "ACCOUNT_ACTIVITY", "Vault Entry " + actionNoun,
        "You " + actionVerb + " the vault entry '" + entry.getTitle() + "' " + preposition + ".");

    return mapToResponse(savedEntry);
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getFavorites(String username) {
    if (isDuressMode()) {
      return java.util.Collections.emptyList();
    }
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsFavoriteTrueAndIsDeletedFalse(user.getId());
    SecretKey key = getSharedKeySafely(user);
    return entries.stream()
        .map(e -> mapToResponseWithKey(e, key))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getEntriesByFolder(String username, Long folderId) {
    if (isDuressMode()) {
      return java.util.Collections.emptyList();
    }
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndFolderIdAndIsDeletedFalse(user.getId(), folderId);
    SecretKey key = getSharedKeySafely(user);
    return entries.stream()
        .map(e -> mapToResponseWithKey(e, key))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getEntriesByCategory(String username, Long categoryId) {
    if (isDuressMode()) {
      return java.util.Collections.emptyList();
    }
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    // Using searchEntries instead since we need to make sure the item isn't
    // deleted,
    // and there is no findByUserIdAndCategoryIdAndIsDeletedFalse method.
    // We could add it, but searchEntries handles it perfectly.
    List<VaultEntry> entries = vaultEntryRepository.searchEntries(
        user.getId(), null, categoryId, null, null, null);

    SecretKey key = getSharedKeySafely(user);
    return entries.stream()
        .map(e -> mapToResponseWithKey(e, key))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getRecentEntries(String username) {
    if (isDuressMode()) {
      return java.util.Collections.emptyList();
    }
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultEntry> entries = vaultEntryRepository
        .findTop10ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user.getId());
    SecretKey key = getSharedKeySafely(user);
    return entries.stream()
        .map(e -> mapToResponseWithKey(e, key))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getRecentlyUsedEntries(String username) {
    if (isDuressMode()) {
      return java.util.Collections.emptyList();
    }
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultEntry> entries = vaultEntryRepository
        .findTop10ByUserIdAndIsDeletedFalseOrderByUpdatedAtDesc(user.getId());
    SecretKey key = getSharedKeySafely(user);
    return entries.stream()
        .map(e -> mapToResponseWithKey(e, key))
        .collect(Collectors.toList());
  }

  @Transactional
  public VaultEntryResponse toggleSensitive(String username, Long entryId) {
    checkDuressMode();
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    checkReadOnlyMode(user);

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    entry.setIsHighlySensitive(!Boolean.TRUE.equals(entry.getIsHighlySensitive()));
    VaultEntry savedEntry = vaultEntryRepository.save(entry);

    securityClient.logAction(username, "ENTRY_UPDATED", "Toggled sensitive flag for entry: " + entry.getTitle());

    return mapToResponse(savedEntry);
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> searchEntries(String username, String keyword, Long categoryId,
      Long folderId, Boolean isFavorite, Boolean isHighlySensitive, String sortBy, String sortDir) {
    if (isDuressMode()) {
      return java.util.Collections.emptyList();
    }
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    List<VaultEntry> entries = vaultEntryRepository.searchEntries(
        user.getId(), keyword, categoryId, folderId, isFavorite, isHighlySensitive);

    Comparator<VaultEntry> comparator = switch (sortBy != null ? sortBy : "title") {
      case "createdAt" ->
        Comparator.comparing(VaultEntry::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
      case "updatedAt" ->
        Comparator.comparing(VaultEntry::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
      default -> Comparator.comparing(VaultEntry::getTitle, String.CASE_INSENSITIVE_ORDER);
    };

    if ("desc".equalsIgnoreCase(sortDir)) {
      comparator = comparator.reversed();
    }

    SecretKey key = getSharedKeySafely(user);
    return entries.stream()
        .sorted(comparator)
        .map(e -> mapToResponseWithKey(e, key))
        .collect(Collectors.toList());
  }

  @Transactional
  public void bulkDelete(String username, List<Long> ids) {
    checkDuressMode();
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    checkReadOnlyMode(user);
    List<VaultEntry> entries = vaultEntryRepository.findAllById(ids);

    entries.stream()
        .filter(e -> !e.getUserId().equals(user.getId()))
        .findAny()
        .ifPresent(e -> {
          throw new ResourceNotFoundException("Vault entry not found or access denied");
        });

    LocalDateTime now = LocalDateTime.now();
    entries.forEach(e -> {
      e.setIsDeleted(true);
      e.setDeletedAt(now);
      securityClient.logAction(username, "ENTRY_DELETED", "Deleted entry: " + e.getTitle());
      notificationClient.createNotification(username, "ACCOUNT_ACTIVITY", "Vault Entry Deleted",
          "You moved the vault entry '" + e.getTitle() + "' to the trash.");
    });
    vaultEntryRepository.saveAll(entries);
  }

  @Transactional(readOnly = true)
  public String getPassword(String username, Long id) {
    System.out.println("getPassword: start - username=" + username + ", id=" + id);
    if (isDuressMode()) {
      return "***encrypted***";
    }

    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    if (Boolean.TRUE.equals(entry.getIsHighlySensitive())) {
      throw new AuthenticationException(
          "Sensitive entry requires authentication");
    }

    securityClient.logAction(username, "PASSWORD_VIEWED", "Viewed password for entry: " + entry.getTitle());

    SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    return encryptionService.decrypt(entry.getPassword(), key);
  }

  @Transactional(readOnly = true)
  public VaultEntryDetailResponse accessSensitiveEntry(String username, Long entryId,
      SensitiveAccessRequest request) {
    if (isDuressMode()) {
      throw new AuthenticationException("Invalid master password");
    }
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(user.getUsername(), request.getMasterPassword()));
    } catch (org.springframework.security.core.AuthenticationException e) {
      throw new AuthenticationException("Invalid master password");
    }

    SecretKey key;
    try {
      key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());
    } catch (Exception e) {
      throw new AuthenticationException("Encryption configuration error");
    }

    if (user.is2faEnabled()) {
      // 2FA is handled prior to this or by the gateway/user-service during authentication.
      // if (request.getOtpToken() == null || request.getOtpToken().isBlank()) {
      //   throw new AuthenticationException("OTP is required");
      // }
      // if (!twoFactorService.verifyCode(user, request.getOtpToken())) {
      //   throw new AuthenticationException("Invalid OTP");
      // }
    }

    String decryptedPassword;
    String decryptedUsername;
    String decryptedNotes;

    try {
      decryptedPassword = encryptionService.decrypt(entry.getPassword(), key);
      decryptedUsername = encryptionService.decrypt(entry.getUsername(), key);
      decryptedNotes = entry.getNotes() != null ? encryptionService.decrypt(entry.getNotes(), key) : null;
    } catch (Exception e) {
      throw new AuthenticationException(
          "Invalid master password or encryption error");
    }

    return mapToDetailResponse(entry, decryptedUsername, decryptedPassword, decryptedNotes);
  }

  private SecretKey getSharedKeySafely(UserVaultDetails user) {
    try {
      return encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());
    } catch (Exception e) {
      return null;
    }
  }

  private VaultEntryResponse mapToResponseWithKey(VaultEntry entry, SecretKey key) {
    int score = 0;
    String label = null;
    if (key != null) {
      try {
        String pwd = encryptionService.decrypt(entry.getPassword(), key);
        if (pwd != null && !pwd.isBlank()) {
          score = passwordStrengthCalculator.calculateScore(pwd);
          label = passwordStrengthCalculator.getStrengthLabel(score);
        }
      } catch (Exception e) {
      }
    }

    return VaultEntryResponse.builder()
        .id(entry.getId())
        .title(entry.getTitle())
        .username("******")
        .websiteUrl(entry.getWebsiteUrl())
        .categoryId(entry.getCategory() != null ? entry.getCategory().getId() : null)
        .categoryName(entry.getCategory() != null ? entry.getCategory().getName() : null)
        .folderId(entry.getFolder() != null ? entry.getFolder().getId() : null)
        .folderName(entry.getFolder() != null ? entry.getFolder().getName() : null)
        .isFavorite(entry.getIsFavorite())
        .createdAt(entry.getCreatedAt())
        .updatedAt(entry.getUpdatedAt())
        .strengthScore(score)
        .strengthLabel(label)
        .build();
  }

  private VaultEntryResponse mapToResponse(VaultEntry entry) {
    UserVaultDetails user = userClient.getUserDetailsById(entry.getUserId());
    SecretKey key = getSharedKeySafely(user);
    return mapToResponseWithKey(entry, key);
  }

  private VaultEntryDetailResponse mapToDetailResponse(VaultEntry entry, String username, String password,
      String notes) {

    int score = 0;
    String label = "Unknown";
    @SuppressWarnings("java:S2068") // Not a hardcoded password, just a placeholder string check
    boolean isPlaceholder = password != null && password.equals("******");
    if (password != null && !isPlaceholder) {
      score = passwordStrengthCalculator.calculateScore(password);
      label = passwordStrengthCalculator.getStrengthLabel(score);
    }

    return VaultEntryDetailResponse.builder()
        .id(entry.getId())
        .title(entry.getTitle())
        .username(username)
        .password(password)
        .websiteUrl(entry.getWebsiteUrl())
        .notes(notes)
        .categoryId(entry.getCategory() != null ? entry.getCategory().getId() : null)
        .categoryName(entry.getCategory() != null ? entry.getCategory().getName() : null)
        .folderId(entry.getFolder() != null ? entry.getFolder().getId() : null)
        .folderName(entry.getFolder() != null ? entry.getFolder().getName() : null)
        .isFavorite(entry.getIsFavorite())
        .isHighlySensitive(entry.getIsHighlySensitive())
        .requiresSensitiveAuth(
            Boolean.TRUE.equals(entry.getIsHighlySensitive()) && (password == null || isPlaceholder))
        .createdAt(entry.getCreatedAt())
        .updatedAt(entry.getUpdatedAt())
        .strengthScore(score)
        .strengthLabel(label)
        .build();
  }

  private void checkReadOnlyMode(UserVaultDetails user) {
    if (user.isReadOnlyMode()) throw new IllegalStateException("Vault is in read-only mode. Disable read-only mode in settings to make changes.");
  }

  @Transactional
  public void bulkInsert(String username, List<VaultEntry> entries) {
    if (entries == null || entries.isEmpty())
      return;

    checkDuressMode();
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    checkReadOnlyMode(user);

    List<VaultEntry> savedEntries = vaultEntryRepository.saveAll(entries);

    savedEntries.forEach(entry -> {
      securityClient.logAction(username, "ENTRY_CREATED", "Imported entry: " + entry.getTitle());
      securityClient.analyzePasswordEntry(mapToPayload(entry));
    });
  }

  private com.revature.vault.client.SecurityClient.VaultEntryPayload mapToPayload(com.revature.vault.model.vault.VaultEntry entry) {
      com.revature.vault.client.SecurityClient.VaultEntryPayload p = new com.revature.vault.client.SecurityClient.VaultEntryPayload();
      p.setId(entry.getId());
      p.setTitle(entry.getTitle());
      p.setUsername(entry.getUsername());
      p.setPassword(entry.getPassword());
      return p;
  }

}

