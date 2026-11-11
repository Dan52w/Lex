package com.example.lexcloud.config;

import com.example.lexcloud.service.FileStorageService;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Límite multipart a nivel de contenedor (S8).
 *
 * <p>El tope de 10 MB ya existía en código
 * ({@link FileStorageService#TAMANIO_MAXIMO_BYTES}), pero se ejecuta
 * DESPUÉS de que Spring parsea el multipart completo: un archivo de
 * varios GB podía agotar disco/memoria antes de ser rechazado. Este bean
 * (que sustituye al de la autoconfiguración de Spring Boot) impone el
 * límite ANTES del parseo: el servlet rechaza la petición con
 * {@code MaxUploadSizeExceededException}, que
 * {@code GlobalExceptionHandler} traduce a 413.</p>
 *
 * <p>Tamaño máximo de archivo: el mismo tope real del servicio de
 * archivos (10 MB, constante única). Tamaño máximo de petición: 12 MB,
 * para dejar margen a los campos del formulario que acompañan al archivo.</p>
 */
@Configuration
public class MultipartConfig {

    /** Tamaño máximo de la petición multipart completa (archivo + campos). */
    public static final long TAMANIO_MAXIMO_PETICION_BYTES = 12_582_912L; // 12 MB

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        return new MultipartConfigElement(
                "",
                FileStorageService.TAMANIO_MAXIMO_BYTES,
                TAMANIO_MAXIMO_PETICION_BYTES,
                0);
    }
}
