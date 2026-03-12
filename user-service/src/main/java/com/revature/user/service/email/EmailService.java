package com.revature.user.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  /**
   * Gap 9 fix: notifies a share recipient that a secure password link has been
   * shared with them.
   *
   * @param toEmail         recipient's email address
   * @param senderUsername  username of the person who created the share
   * @param shareUrl        the full share URL (including fragment key)
   * @param expiresAt       when the share expires, formatted for display
   */
  @Async
  public void sendShareNotificationEmail(String toEmail, String senderUsername,
                                         String shareUrl, String expiresAt) {
    log.info("Sending share notification email to: {}", toEmail);
    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(senderUsername + " shared a secure password with you — Rev-PasswordManager");

      String content = String.format(
          "<h3>Rev-PasswordManager — Secure Password Share</h3>" +
          "<p><strong>%s</strong> has shared a secure, time-limited password link with you.</p>" +
          "<p><a href=\"%s\">Click here to view the shared password</a></p>" +
          "<p><strong>This link expires at: %s</strong></p>" +
          "<p>For security, this link can only be viewed a limited number of times. " +
          "Do not forward it to others.</p>" +
          "<p>If you did not expect this, please ignore this email.</p>",
          senderUsername, shareUrl, expiresAt);

      helper.setText(content, true);
      javaMailSender.send(message);
      log.info("Share notification email sent successfully to: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Failed to send share notification email to {}: {}", toEmail, e.getMessage());
      // Non-fatal: share creation should not fail if email fails
    }
  }

  /**
   * Sends a simple plain-text email. Used by Feature 39 (Emergency Access) and other features
   * that need to send ad-hoc notifications.
   *
   * @param toEmail  recipient email address
   * @param subject  email subject
   * @param body     plain-text body
   */
  @Async
  public void sendSimpleEmail(String toEmail, String subject, String body) {
    log.info("Sending simple email to: {} subject: {}", toEmail, subject);
    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText("<p>" + body.replace("\n", "<br>") + "</p>", true);
      javaMailSender.send(message);
      log.info("Simple email sent successfully to: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Failed to send simple email to {}: {}", toEmail, e.getMessage());
      // Non-fatal: caller should handle gracefully
    }
  }

  @Async
  public void sendOtpEmail(String toEmail, String otpCode) {
    log.info("Sending OTP email to: {}", toEmail);
    // For manual local testing without SMTP:
    log.info("============== INTERCEPTED OTP CODE: {} ==============", otpCode);
    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject("Your Security Code - Rev-PasswordManager");

      String content = String.format(
          "<h3>Rev-PasswordManager Security</h3>" +
              "<p>Your security verification code is:</p>" +
              "<h1>%s</h1>" +
              "<p>This code will expire in 15 minutes.</p>" +
              "<p>If you did not request this code, please ignore this email.</p>",
          otpCode);

      helper.setText(content, true);

      javaMailSender.send(message);
      log.info("OTP email sent successfully to: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Failed to send OTP email", e);
      throw new RuntimeException("Failed to send email");
    }
  }
}
