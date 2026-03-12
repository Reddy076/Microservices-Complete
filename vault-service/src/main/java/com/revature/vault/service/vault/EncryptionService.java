package com.revature.vault.service.vault;

import com.revature.vault.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {

  private final EncryptionUtil encryptionUtil;

  public String encrypt(String data, SecretKey key) {
    try {
      return encryptionUtil.encrypt(data, key);
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while encrypting data", e);
    }
  }

  public String decrypt(String encryptedData, SecretKey key) {
    try {
      return encryptionUtil.decrypt(encryptedData, key);
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while decrypting data", e);
    }
  }

  public SecretKey generateNewKey() {
    try {
      return encryptionUtil.generateKey();
    } catch (Exception e) {
      throw new RuntimeException("Error generating encryption key", e);
    }
  }

  public String encodeKey(SecretKey key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

  public SecretKey decodeKey(String keyString) {
    try {
      // Try Base64 decoding first (for backward compatibility)
      byte[] decodedKey = Base64.getDecoder().decode(keyString);
      return encryptionUtil.getKeyFromBytes(decodedKey);
    } catch (IllegalArgumentException e) {
      // If not Base64, treat as UUID string (remove hyphens and convert from hex)
      String hexString = keyString.replace("-", "");
      byte[] decodedKey = hexStringToBytes(hexString);
      return encryptionUtil.getKeyFromBytes(decodedKey);
    }
  }

  private byte[] hexStringToBytes(String hexString) {
    int len = hexString.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
          + Character.digit(hexString.charAt(i + 1), 16));
    }
    return data;
  }

  public String encryptForExport(String data, String password, String salt) {
    try {
      SecretKey key = encryptionUtil.deriveKey(password, salt);
      return encryptionUtil.encrypt(data, key);
    } catch (Exception e) {
      throw new RuntimeException("Error encrypting export data", e);
    }
  }

  public String decryptForImport(String encryptedData, String password, String salt) {
    try {
      SecretKey key = encryptionUtil.deriveKey(password, salt);
      return encryptionUtil.decrypt(encryptedData, key);
    } catch (Exception e) {
      throw new RuntimeException("Error decrypting import data", e);
    }
  }

  /**
   * Derives a SecretKey from a password and salt using PBKDF2.
   * This is the proper way to get the encryption key for vault operations.
   */
  public SecretKey deriveKey(String password, String salt) {
    return encryptionUtil.deriveKey(password, salt);
  }
}

