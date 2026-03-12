package com.revature.vault.service.sharing;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles one-time AES-256-GCM encryption for secure shares.
 *
 * <p>
 * The encryption key is generated fresh for each share and returned to the
 * caller
 * to be embedded in the share URL fragment. It is <em>never persisted</em> on
 * the server —
 * only the ciphertext and IV are stored in the database.
 */
@Service
public class ShareEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_SIZE = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Encrypts plaintext with a freshly generated AES-256-GCM key.
     *
     * @param plaintext the data to encrypt
     * @return a {@link ShareEncryptionResult} containing ciphertext, IV, and the
     *         raw key
     */
    public ShareEncryptionResult encrypt(String plaintext) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE, SECURE_RANDOM);
            SecretKey key = keyGen.generateKey();

            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            return ShareEncryptionResult.builder()
                    .encryptedData(Base64.getEncoder().encodeToString(ciphertext))
                    .iv(Base64.getEncoder().encodeToString(iv))
                    .keyBase64(Base64.getEncoder().encodeToString(key.getEncoded()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Share encryption failed", e);
        }
    }

    /**
     * Decrypts ciphertext using the provided Base64-encoded key and IV.
     *
     * @param encryptedData Base64-encoded ciphertext
     * @param ivBase64      Base64-encoded IV
     * @param keyBase64     Base64-encoded AES-256 key
     * @return decrypted plaintext
     */
    public String decrypt(String encryptedData, String ivBase64, String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] ciphertext = Base64.getDecoder().decode(encryptedData);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new RuntimeException("Share decryption failed", e);
        }
    }

    /**
     * Result of a one-time share encryption operation.
     */
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ShareEncryptionResult {
        private String encryptedData;
        private String iv;
        /**
         * The one-time AES key in Base64 — must be sent to the client, never stored
         * server-side
         */
        private String keyBase64;
    }
}
