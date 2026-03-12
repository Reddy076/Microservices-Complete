package com.revature.vault.util;

import com.revature.vault.config.EncryptionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class EncryptionUtil {

  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private final EncryptionConfig encryptionConfig;

  public String encrypt(String data, SecretKey key) throws Exception {
    byte[] iv = new byte[GCM_IV_LENGTH];
    SECURE_RANDOM.nextBytes(iv);

    Cipher cipher = Cipher.getInstance(encryptionConfig.getAlgorithm());
    GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

    byte[] cipherText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

    byte[] encryptedData = new byte[GCM_IV_LENGTH + cipherText.length];
    System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
    System.arraycopy(cipherText, 0, encryptedData, GCM_IV_LENGTH, cipherText.length);

    return Base64.getEncoder().encodeToString(encryptedData);
  }

  public String decrypt(String encryptedData, SecretKey key) throws Exception {
    if (encryptedData == null || encryptedData.isBlank()) {
      return "";
    }
    byte[] decodedData = Base64.getDecoder().decode(encryptedData);

    byte[] iv = new byte[GCM_IV_LENGTH];
    System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);

    byte[] cipherText = new byte[decodedData.length - GCM_IV_LENGTH];
    System.arraycopy(decodedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

    Cipher cipher = Cipher.getInstance(encryptionConfig.getAlgorithm());
    GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
    cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

    byte[] plainText = cipher.doFinal(cipherText);
    return new String(plainText, StandardCharsets.UTF_8);
  }

  public SecretKey generateKey() throws NoSuchAlgorithmException {

    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(encryptionConfig.getKeySize());
    return keyGenerator.generateKey();
  }

  public SecretKey getKeyFromBytes(byte[] keyBytes) {
    return new SecretKeySpec(keyBytes, "AES");
  }

  public SecretKey deriveKey(String password, String salt) {
    try {
      javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

      int iterations = 100000;

      int keyLength = encryptionConfig.getKeySize();

      javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
          password.toCharArray(),
          salt.getBytes(StandardCharsets.UTF_8),
          iterations,
          keyLength);

      byte[] keyBytes = factory.generateSecret(spec).getEncoded();
      return new SecretKeySpec(keyBytes, "AES");
    } catch (Exception e) {
      throw new RuntimeException("Error deriving key from password", e);
    }
  }
}
