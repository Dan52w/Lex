package com.example.lexcloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos en el login (S3): fuerza bruta / credential stuffing
 * contra {@code POST /api/auth/login}.
 *
 * <p>Regla: 10 intentos por minuto por IP; al superarlo responde 429 con
 * el formato {@code ErrorResponse} ({@code status}, {@code message},
 * {@code timestamp}) y header {@code Retry-After} en segundos. Solo
 * aplica a ese endpoint: cualquier otra petición pasa sin tocarse.</p>
 *
 * <p>Implementación simple y sin dependencias: {@link ConcurrentHashMap}
 * de IP a marcas de tiempo, con ventana deslizante de 60 segundos. La IP
 * es la del cliente directo ({@code getRemoteAddr}); si el backend queda
 * detrás de un proxy que no reenvía la IP real, todas las peticiones
 * compartirían el mismo contador (limitación conocida, documentada).</p>
 *
 * <p>Cuenta TODOS los intentos (válidos o no): distinguirlos exigiría
 * inspeccionar la respuesta de autenticación. 10/minuto por IP es amplio
 * para uso legítimo y estrecho para un ataque automatizado.</p>
 */
public class RateLimitLoginFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitLoginFilter.class);

    /** Intentos permitidos por ventana y por IP. */
    public static final int MAX_INTENTOS = 10;

    /** Ventana deslizante en milisegundos (1 minuto). */
    public static final long VENTANA_MS = 60_000L;

    /** Límite de entradas del mapa antes de una limpieza oportunista. */
    private static final int MAX_IPS_RASTREADAS = 10_000;

    private final ConcurrentHashMap<String, ArrayDeque<Long>> intentosPorIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().endsWith("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        long ahora = System.currentTimeMillis();
        ArrayDeque<Long> intentos = intentosPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        long retryAfterSegundos = 0;
        synchronized (intentos) {
            while (!intentos.isEmpty() && ahora - intentos.peekFirst() > VENTANA_MS) {
                intentos.pollFirst();
            }
            if (intentos.size() >= MAX_INTENTOS) {
                retryAfterSegundos = Math.max(1, (VENTANA_MS - (ahora - intentos.peekFirst())) / 1000);
            } else {
                intentos.addLast(ahora);
            }
        }

        if (retryAfterSegundos > 0) {
            log.warn("Login bloqueado por rate limiting (IP {})", ip);
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", String.valueOf(retryAfterSegundos));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(String.format(
                    "{\"status\":429,\"message\":\"Demasiados intentos de inicio de sesión. Intenta de nuevo en %d segundos.\",\"timestamp\":\"%s\"}",
                    retryAfterSegundos, LocalDateTime.now()));
            return;
        }

        if (intentosPorIp.size() > MAX_IPS_RASTREADAS) {
            limpiarVacios();
        }
        filterChain.doFilter(request, response);
    }

    /** Elimina las entradas sin intentos vigentes (evita crecimiento sin límite del mapa). */
    private void limpiarVacios() {
        intentosPorIp.entrySet().removeIf(entrada -> {
            ArrayDeque<Long> intentos = entrada.getValue();
            synchronized (intentos) {
                return intentos.isEmpty();
            }
        });
    }
}
