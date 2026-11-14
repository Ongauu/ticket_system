package com.crimson.auth.config;

import com.crimson.auth.dto.response.ApiError;
import com.crimson.auth.security.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter extends OncePerRequestFilter {
    private record Window(long minute, AtomicInteger count){}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limitPerMinute;
    private final ObjectMapper objectMapper;

    public RateLimiter(int limitPerMinute, ObjectMapper objectMapper){
        this.limitPerMinute = limitPerMinute;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        return !HttpMethod.POST.matches(request.getMethod()) || !request.getRequestURI().startsWith("/api/reservations");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        long minute = System.currentTimeMillis() / 60_000;
        Window w = windows.compute(clientKey(request), (k, cur) -> cur == null || cur.minute() != minute ? new Window(minute, new AtomicInteger()) : cur);

        if(w.count().incrementAndGet() > limitPerMinute){
            long retryAfter = 60 - (System.currentTimeMillis() / 1000 % 60);
            ApiError error = ApiError.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("Too Many Requests")
                    .message("Rate limit exceeded. Retry in " + retryAfter + "s")
                    .path(request.getRequestURI())
                    .build();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        if(windows.size() > 100_000){
            windows.entrySet().removeIf(e -> e.getValue().minute() < minute);
        }
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl u) {
            return "u:" + u.getId();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
