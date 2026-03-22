package com.revature.user.service.user;

import com.revature.user.dto.request.ChangePasswordRequest;
import com.revature.user.dto.request.UpdateProfileRequest;
import com.revature.user.dto.response.UserResponse;
import com.revature.user.exception.AuthenticationException;
import com.revature.user.model.user.User;
import com.revature.user.client.VaultClient;
import com.revature.user.repository.UserRepository;
import com.revature.user.util.EncryptionUtil;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final VaultClient vaultClient;
  private final EncryptionUtil encryptionUtil;
  private final com.revature.user.client.SecurityClient securityClient;

  @Transactional(readOnly = true)
  public UserResponse getUserProfile(String username) {
    User user = userRepository.findByUsernameOrThrow(username);

    return UserResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .name(user.getName())
        .phoneNumber(user.getPhoneNumber())
        .is2faEnabled(user.is2faEnabled())
        .createdAt(user.getCreatedAt())
        .deletionScheduledAt(user.getDeletionScheduledAt())
        .build();
  }

  @Transactional
  public UserResponse updateProfile(String username, UpdateProfileRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    if (request.getName() != null) {
      user.setName(request.getName());
    }
    if (request.getPhoneNumber() != null) {
      user.setPhoneNumber(request.getPhoneNumber());
    }
    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("Email address is already in use by another account.");
      }
      user.setEmail(request.getEmail());
    }

    User savedUser = userRepository.save(user);

    return UserResponse.builder()
        .id(savedUser.getId())
        .email(savedUser.getEmail())
        .username(savedUser.getUsername())
        .name(savedUser.getName())
        .phoneNumber(savedUser.getPhoneNumber())
        .is2faEnabled(savedUser.is2faEnabled())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }

  @Transactional
  public void changeMasterPassword(String username, ChangePasswordRequest request) {
    log.info("Starting master password change for user: {}", username);
    User user = userRepository.findByUsernameOrThrow(username);

    if (!passwordEncoder.matches(request.getOldPassword(), user.getMasterPasswordHash())) {
      log.warn("Old password verification failed for user: {}", username);
      throw new AuthenticationException("Invalid old password");
    }

    // Derive the OLD encryption key from the current master password hash
    SecretKey oldKey = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());
    log.info("Derived old encryption key for user: {}", username);

    // Update the master password hash to the NEW password
    String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
    String oldHashPrefix = user.getMasterPasswordHash().substring(0,
        Math.min(20, user.getMasterPasswordHash().length()));
    String newHashPrefix = newPasswordHash.substring(0, Math.min(20, newPasswordHash.length()));
    log.info("Updating password hash from [{}...] to [{}...]", oldHashPrefix, newHashPrefix);
    user.setMasterPasswordHash(newPasswordHash);

    // Derive the NEW encryption key from the new master password hash
    SecretKey newKey = encryptionUtil.deriveKey(newPasswordHash, user.getSalt());
    log.info("Derived new encryption key for user: {}", username);

    // Call Vault Service to re-encrypt
    Map<String, String> keys = new HashMap<>();
    keys.put("oldKey", Base64.getEncoder().encodeToString(oldKey.getEncoded()));
    keys.put("newKey", Base64.getEncoder().encodeToString(newKey.getEncoded()));
    vaultClient.reencryptVault(user.getId(), keys);
    
    // Save the user with new password hash
    userRepository.save(user);
    log.info("Master password change completed successfully for user: {}", username);

    // Fire security alert
    try {
      securityClient.createAlert(username,
          "PASSWORD_CHANGED",
          "Master Password Changed",
          "Your master password was successfully changed. If you didn't do this, contact support immediately.",
          "HIGH");
    } catch (Exception ex) {
      log.warn("Could not send password-change alert for user {}: {}", username, ex.getMessage());
    }
  }
}
