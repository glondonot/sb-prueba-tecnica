package com.segurosbolivar.polizas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;

/**
 * Exige el header {@code api-key} en todas las rutas de negocio.
 * Quedan públicas solo la documentación OpenAPI y el health check (necesario para orquestadores).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private static final List<String> RUTAS_PUBLICAS = List.of(
            "/swagger-ui", "/v3/api-docs", "/actuator/health");

    private final SeguridadProperties properties;
    private final byte[] apiKeyEsperada;

    public ApiKeyFilter(SeguridadProperties properties) {
        this.properties = properties;
        this.apiKeyEsperada = properties.apiKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return RUTAS_PUBLICAS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(properties.apiKeyHeader());
        if (apiKey == null || !esValida(apiKey)) {
            log.warn("Acceso rechazado: api-key {} en {} {}",
                    apiKey == null ? "ausente" : "inválida", request.getMethod(), request.getRequestURI());
            rechazar(request, response, apiKey == null);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Comparación en tiempo constante para no filtrar información por diferencias de tiempo. */
    private boolean esValida(String apiKey) {
        return MessageDigest.isEqual(apiKeyEsperada, apiKey.getBytes(StandardCharsets.UTF_8));
    }

    private void rechazar(HttpServletRequest request, HttpServletResponse response, boolean ausente)
            throws IOException {
        String detalle = ausente
                ? "Debe enviar el header '%s'".formatted(properties.apiKeyHeader())
                : "El valor del header '%s' no es válido".formatted(properties.apiKeyHeader());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"No autorizado","status":401,"detail":"%s","instance":"%s","timestamp":"%s"}"""
                .formatted(detalle, request.getRequestURI(), Instant.now()));
    }
}
