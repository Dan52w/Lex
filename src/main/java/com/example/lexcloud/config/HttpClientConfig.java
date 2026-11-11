package com.example.lexcloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cliente HTTP compartido (A13).
 *
 * <p>Un único {@link RestTemplate} para toda la aplicación, con timeouts
 * explícitos: sin ellos, una llamada a un servicio externo colgado deja el
 * hilo del request esperando indefinidamente. Los servicios deben inyectar
 * este bean y nunca instanciar uno nuevo por llamada.</p>
 *
 * <p>Spring Boot 4 eliminó {@code RestTemplateBuilder}; el equivalente es
 * construir la factoría directamente y pasarla al constructor.</p>
 */
@Configuration
public class HttpClientConfig {

    /** Tiempo máximo para establecer la conexión. */
    public static final Duration CONNECT_TIMEOUT = Duration.ofMillis(5000);

    /** Tiempo máximo esperando la respuesta una vez conectado. */
    public static final Duration READ_TIMEOUT = Duration.ofMillis(30000);

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
    }
}
