package com.revature.gateway.filter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global rate-limiting filter for the API Gateway.
 * Adapted from the monolith's RateLimitFilter (servlet) to Spring Cloud Gateway GlobalFilter (reactive).
 *
 * <p>Limits:
 * <ul>
 *   <li>Auth endpoints (/api/auth/**): 10 requests/minute per IP</li>
 *   <li>All other endpoints: 60 requests/minute per IP</li>
 * </ul>
 * Returns HTTP 429 with JSON body and an X-RateLimit-Remaining header on all passing requests.
 */
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitGlobalFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE      = 60;
    private static final int AUTH_MAX_REQUESTS_PER_MINUTE = 10;

    private static final String TOO_MANY_REQUESTS_BODY =
            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}";

    // One Guava cache per traffic class — expire entries after 1 minute of no writes (same as monolith)
    private final LoadingCache<String, AtomicInteger> generalRequestCounts;
    private final LoadingCache<String, AtomicInteger> authRequestCounts;

    public RateLimitGlobalFilter() {
        generalRequestCounts = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(CacheLoader.from(() -> new AtomicInteger(0)));

        authRequestCounts = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(CacheLoader.from(() -> new AtomicInteger(0)));
    }

    // Run BEFORE JwtAuthFilter (which is -1 in Gateway ordering) — reject rate-limited requests first
    @Override
    public int getOrder() {
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ipAddress = resolveClientIp(request);
        String path = request.getURI().getPath();

        if (!isAllowed(ipAddress, path)) {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", ipAddress, path);
            return rejectRequest(exchange);
        }

        int remaining = getRemainingRequests(ipAddress, path);
        ServerHttpRequest mutated = request.mutate()
                .header("X-RateLimit-Remaining", String.valueOf(remaining))
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutated).build();
        mutatedExchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));

        return chain.filter(mutatedExchange);
    }

    // ── Rate check helpers ─────────────────────────────────────────────────────

    private boolean isAllowed(String ipAddress, String path) {
        try {
            if (path != null && path.startsWith("/api/auth")) {
                AtomicInteger count = authRequestCounts.get(ipAddress);
                return count.incrementAndGet() <= AUTH_MAX_REQUESTS_PER_MINUTE;
            }
            AtomicInteger count = generalRequestCounts.get(ipAddress);
            return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        } catch (ExecutionException e) {
            return true; // fail open — don't block if cache fails
        }
    }

    private int getRemainingRequests(String ipAddress, String path) {
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

    // ── IP resolution ──────────────────────────────────────────────────────────

    private String resolveClientIp(ServerHttpRequest request) {
        // Respect X-Forwarded-For for reverse-proxy setups (same logic as ClientIpUtil in monolith)
        List<String> xForwardedFor = request.getHeaders().get("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String firstIp = xForwardedFor.get(0).split(",")[0].trim();
            if (!firstIp.isBlank()) return firstIp;
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    // ── 429 response ──────────────────────────────────────────────────────────

    private Mono<Void> rejectRequest(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = TOO_MANY_REQUESTS_BODY.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
