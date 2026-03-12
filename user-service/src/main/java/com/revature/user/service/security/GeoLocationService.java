package com.revature.user.service.security;

import com.revature.user.util.ClientIpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GeoLocationService {

  private static final Logger logger = LoggerFactory.getLogger(GeoLocationService.class);

  private final ClientIpUtil clientIpUtil;

  public GeoLocationService(ClientIpUtil clientIpUtil) {
    this.clientIpUtil = clientIpUtil;
  }

  public String getLocationFromIp(String ipAddress) {
    // Use ClientIpUtil for consistent localhost detection
    if (clientIpUtil.isLocalhost(ipAddress)) {
      return "Local Network";
    }

    if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
      return "Unknown";
    }

    logger.debug("Simulated GeoLocation lookup for IP: {}", ipAddress);
    return "City, Country (Simulated)";
  }
}
