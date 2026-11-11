package com.example.lexcloud.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cliente HTTP compartido (A13).
 *
 * <p>Un único {@link RestTemplate} para toda la aplicación, con timeouts
 * explícitos: sin ellos, una llamada a un servicio externo colgado deja el
 * hilo del request esperando indefinidamente. Los servicios deben inyectar
 * este bean y nunca instanciar uno nuevo por llamada.</p>
 */
@Configuration
public class HttpClientConfig {

    /** Tiempo máximo para establecer la conexión. */
    public static final Duration CONNECT_TIMEOUT = Duration.ofMillis(5000);

    /** Tiempo máximo esperando la respuesta una vez conectado. */
    public static final Duration READ_TIMEOUT = Duration.ofMillis(30000);

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .build();
    }
}
