package com.smartclinic.hms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP 기반 Rate Limiting 필터 — 토큰 버킷 방식
 *
 * 엔드포인트별 차등 제한:
 *   POST /login      : 10회/분 (브루트포스 방지)
 *   /llm/symptom/**  : 20회/분 (API 비용 제어)
 *   기타             : 100회/분
 *
 * 초과 시 429 Too Many Requests + JSON 에러 응답 반환.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${hms.rate-limit.trust-proxy:false}")
    private boolean trustProxy;

    private static final int LOGIN_LIMIT = 10;
    private static final int LLM_SYMPTOM_LIMIT = 20;
    private static final int DEFAULT_LIMIT = 100;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        int limit = resolveLimit(path, method);
        String bucketKey = clientIp + ":" + resolveBucketCategory(path, method);

        AtomicInteger capturedCount = new AtomicInteger();
        buckets.compute(bucketKey, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                capturedCount.set(1);
                return new Bucket(now, 1);
            }
            existing.count++;
            capturedCount.set(existing.count);
            return existing;
        });

        if (capturedCount.get() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String safePath = path.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "")
                    .replace("\r", "");

            String json = "{\"success\":false,"
                    + "\"errorCode\":\"RATE_LIMIT_EXCEEDED\","
                    + "\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.\","
                    + "\"timestamp\":\"" + Instant.now() + "\","
                    + "\"path\":\"" + safePath + "\"}";

            response.getWriter().write(json);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && "/login".equals(path)) {
            return LOGIN_LIMIT;
        }
        if (path.startsWith("/llm/symptom")) {
            return LLM_SYMPTOM_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    private String resolveBucketCategory(String path, String method) {
        if ("POST".equalsIgnoreCase(method) && "/login".equals(path)) {
            return "login";
        }
        if (path.startsWith("/llm/symptom")) {
            return "llm-symptom";
        }
        return "default";
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustProxy) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** 만료된 버킷 정리 — 2분마다 실행 */
    @Scheduled(fixedRate = 120_000)
    public void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS * 2);
    }

    /** 정적 리소스는 Rate Limit 제외 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/h2-console");
    }

    private static class Bucket {
        long windowStart;
        int count;

        Bucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
