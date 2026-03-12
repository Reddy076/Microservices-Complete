package com.revature.vault.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Utility class for extracting the real client IP address from HTTP requests.
 * Handles various proxy headers and falls back to remote address.
 */
@Component
public class ClientIpUtil {

    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "REMOTE_ADDR"
    };

    /**
     * Extracts the client IP address from the request, checking various proxy headers.
     *
     * @param request the HTTP request
     * @return the client IP address, or "unknown" if not found
     */
    public String getClientIpAddress(HttpServletRequest request) {
        // Check all proxy headers first
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can have multiple IPs (client, proxy1, proxy2...), take the first one
                return ip.split(",")[0].trim();
            }
        }

        // Fall back to remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Checks if the IP address is a localhost or private network address.
     *
     * @param ipAddress the IP address to check
     * @return true if localhost or private network
     */
    public boolean isLocalhost(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }

        return ipAddress.equals("127.0.0.1") ||
               ipAddress.equals("0:0:0:0:0:0:0:1") ||
               ipAddress.equals("::1") ||
               ipAddress.startsWith("192.168.") ||
               ipAddress.startsWith("10.") ||
               ipAddress.startsWith("172.16.") ||
               ipAddress.startsWith("172.17.") ||
               ipAddress.startsWith("172.18.") ||
               ipAddress.startsWith("172.19.") ||
               ipAddress.startsWith("172.20.") ||
               ipAddress.startsWith("172.21.") ||
               ipAddress.startsWith("172.22.") ||
               ipAddress.startsWith("172.23.") ||
               ipAddress.startsWith("172.24.") ||
               ipAddress.startsWith("172.25.") ||
               ipAddress.startsWith("172.26.") ||
               ipAddress.startsWith("172.27.") ||
               ipAddress.startsWith("172.28.") ||
               ipAddress.startsWith("172.29.") ||
               ipAddress.startsWith("172.30.") ||
               ipAddress.startsWith("172.31.") ||
               ipAddress.startsWith("fc00:") ||
               ipAddress.startsWith("fe80:");
    }

    /**
     * Gets a display-friendly location string for an IP address.
     * Returns "Local Network" for private IPs, "Unknown" for null/empty.
     *
     * @param ipAddress the IP address
     * @return display-friendly location
     */
    public String getDisplayLocation(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            return "Unknown";
        }

        if (isLocalhost(ipAddress)) {
            return "Local Network";
        }

        return "External";
    }
}
