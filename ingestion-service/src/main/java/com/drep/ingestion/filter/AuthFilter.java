package com.drep.ingestion.filter;

import com.drep.ingestion.auth.JwtService;
import com.drep.ingestion.config.IngestionProperties;
import com.drep.common.dto.ApiErrorResponse;
import com.drep.ingestion.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String TENANT_ID_ATTR = "authenticatedTenantId";

    private final IngestionProperties properties;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AuthFilter(IngestionProperties properties, JwtService jwtService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/health".equals(path) || path.startsWith("/actuator/health") || path.startsWith("/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = TraceIdFilter.getTraceId(request);

        try {
            String tenantId = authenticate(request);
            request.setAttribute(TENANT_ID_ATTR, tenantId);
            MDC.put("tenantId", tenantId);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    tenantId, null, List.of(new SimpleGrantedAuthority("ROLE_INGEST")));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (UnauthorizedException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(TraceIdFilter.TRACE_ID_HEADER, traceId);
            objectMapper.writeValue(response.getOutputStream(),
                    ApiErrorResponse.of("UNAUTHORIZED", ex.getMessage(), traceId));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String authenticate(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            String tenantId = properties.getApiKeys().get(apiKey);
            if (tenantId != null) {
                return tenantId;
            }
            throw new UnauthorizedException("Invalid API key");
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.validateAndExtractTenant(token)
                    .orElseThrow(() -> new UnauthorizedException("Invalid or expired JWT"));
        }

        throw new UnauthorizedException("Missing authentication: provide X-API-Key or Bearer JWT");
    }

    public static String getAuthenticatedTenantId(HttpServletRequest request) {
        Object tenantId = request.getAttribute(TENANT_ID_ATTR);
        return tenantId != null ? tenantId.toString() : null;
    }
}
