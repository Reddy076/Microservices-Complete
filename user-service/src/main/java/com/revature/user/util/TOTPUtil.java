package com.revature.user.util;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Component;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Component
public class TOTPUtil {

  private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
  private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
  private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
  private final TimeProvider timeProvider = new SystemTimeProvider();
  private final DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

  public String generateSecret() {
    return secretGenerator.generate();
  }

  public String getQrCodeUrl(String secret, String email) throws QrGenerationException {
    QrData data = new QrData.Builder()
        .label(email)
        .secret(secret)
        .issuer("Rev-PasswordManager")
        .algorithm(HashingAlgorithm.SHA1)
        .digits(6)
        .period(30)
        .build();

    return getDataUriForImage(qrGenerator.generate(data), qrGenerator.getImageMimeType());
  }

  public boolean verifyCode(String secret, String code) {
    return codeVerifier.isValidCode(secret, code);
  }
}
