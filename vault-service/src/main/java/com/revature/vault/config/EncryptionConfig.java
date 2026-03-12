package com.revature.vault.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "encryption")
@Data
public class EncryptionConfig {
  private String algorithm;
  private int keySize;
}
