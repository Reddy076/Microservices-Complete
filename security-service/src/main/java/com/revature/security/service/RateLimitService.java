package com.revature.security.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

  private static final int MAX_REQUESTS_PER_MINUTE = 60;
  private static final int AUTH_MAX_REQUESTS_PER_MINUTE = 10;

  private final LoadingCache<String, AtomicInteger> generalRequestCounts;
  private final LoadingCache<String, AtomicInteger> authRequestCounts;

  public RateLimitService() {
    generalRequestCounts = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(CacheLoader.from(() -> new AtomicInteger(0)));

    authRequestCounts = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(CacheLoader.from(() -> new AtomicInteger(0)));
  }

  public boolean isAllowed(String ipAddress, String path) {
    try {
      if (path != null && path.startsWith("/api/auth")) {
        AtomicInteger count = authRequestCounts.get(ipAddress);
        return count.incrementAndGet() <= AUTH_MAX_REQUESTS_PER_MINUTE;
      }
      AtomicInteger count = generalRequestCounts.get(ipAddress);
      return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
    } catch (ExecutionException e) {
      return true;
    }
  }

  public int getRemainingRequests(String ipAddress, String path) {
    try {
      if (path != null && path.startsWith("/api/auth")) {
        AtomicInteger count = authRequestCounts.get(ipAddress);
        return Math.max(0, AUTH_MAX_REQUESTS_PER_MINUTE - count.get());
      }
      AtomicInteger count = generalRequestCounts.get(ipAddress);
      return Math.max(0, MAX_REQUESTS_PER_MINUTE - count.get());
    } catch (ExecutionException e) {
      return MAX_REQUESTS_PER_MINUTE;
    }
  }
}






