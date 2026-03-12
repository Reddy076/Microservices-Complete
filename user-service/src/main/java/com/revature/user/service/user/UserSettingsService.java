package com.revature.user.service.user;

import com.revature.user.dto.request.UserSettingsRequest;
import com.revature.user.dto.response.UserSettingsResponse;
import com.revature.user.model.user.User;
import com.revature.user.model.user.UserSettings;
import com.revature.user.repository.UserRepository;
import com.revature.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

  private final UserSettingsRepository userSettingsRepository;
  private final UserRepository userRepository;

  @Transactional
  public UserSettingsResponse getSettings(String username) {
    User user = userRepository.findByUsernameOrThrow(username);

    UserSettings settings = userSettingsRepository.findByUserId(user.getId())
        .orElseGet(() -> createDefaultSettings(user));

    return mapToResponse(settings);
  }

  @Transactional
  public UserSettingsResponse updateSettings(String username, UserSettingsRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    UserSettings settings = userSettingsRepository.findByUserId(user.getId())
        .orElseGet(() -> createDefaultSettings(user));

    if (request.getTheme() != null)
      settings.setTheme(request.getTheme());
    if (request.getLanguage() != null)
      settings.setLanguage(request.getLanguage());
    if (request.getAutoLogoutMinutes() != null)
      settings.setAutoLogoutMinutes(request.getAutoLogoutMinutes());
    if (request.getReadOnlyMode() != null)
      settings.setReadOnlyMode(request.getReadOnlyMode());

    UserSettings savedSettings = userSettingsRepository.save(settings);
    return mapToResponse(savedSettings);
  }

  private UserSettings createDefaultSettings(User user) {
    UserSettings settings = UserSettings.builder()
        .user(user)
        .theme("SYSTEM")
        .language("en-US")
        .autoLogoutMinutes(15)
        .readOnlyMode(false)
        .build();
    return userSettingsRepository.save(settings);
  }

  private UserSettingsResponse mapToResponse(UserSettings settings) {
    return UserSettingsResponse.builder()
        .theme(settings.getTheme())
        .language(settings.getLanguage())
        .autoLogoutMinutes(settings.getAutoLogoutMinutes())
        .readOnlyMode(settings.getReadOnlyMode())
        .build();
  }

  @Transactional
  public UserSettingsResponse toggleReadOnlyMode(String username, Boolean isReadOnly) {
    User user = userRepository.findByUsernameOrThrow(username);

    UserSettings settings = userSettingsRepository.findByUserId(user.getId())
        .orElseGet(() -> createDefaultSettings(user));

    settings.setReadOnlyMode(isReadOnly);
    UserSettings savedSettings = userSettingsRepository.save(settings);
    return mapToResponse(savedSettings);
  }
}
