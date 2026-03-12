package com.revature.vault.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class DateTimeUtil {

  public boolean isUnusualTime(LocalDateTime loginTime) {
    if (loginTime == null) {
      return false;
    }

    LocalTime time = loginTime.toLocalTime();

    LocalTime startUnusual = LocalTime.of(2, 0);
    LocalTime endUnusual = LocalTime.of(5, 0);

    return time.isAfter(startUnusual) && time.isBefore(endUnusual);
  }
}
