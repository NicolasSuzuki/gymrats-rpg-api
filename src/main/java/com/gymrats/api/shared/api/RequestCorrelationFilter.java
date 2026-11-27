package com.gymrats.api.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
  private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    var supplied = request.getHeader("X-Request-Id");
    var requestId = supplied != null && SAFE_ID.matcher(supplied).matches()
        ? supplied : UUID.randomUUID().toString();
    var previous = MDC.get("requestId");
    long start = System.nanoTime();
    MDC.put("requestId", requestId);
    response.setHeader("X-Request-Id", requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      // Never include headers, query strings, user data, bodies or image names in this event.
      log.info("requestId={} method={} status={} durationMs={}", requestId,
          request.getMethod(), response.getStatus(), (System.nanoTime() - start) / 1_000_000);
      if (previous == null) MDC.remove("requestId"); else MDC.put("requestId", previous);
    }
  }
}
