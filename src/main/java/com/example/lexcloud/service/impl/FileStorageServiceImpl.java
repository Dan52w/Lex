package com.example.lexcloud.service.impl;

import com.example.lexcloud.security.FileValidation;
import com.example.lexcloud.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    // Se inyectan de manera dinámica desde el application.properties
    @Value("${supabase.storage.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.key}")
    private String supabaseKey;

    @Override
    public String guardarArchivo(MultipartFile archivo, String nombreCarpeta, String nombreArchivo) {
        try {
            // P0-B5: la carpeta y el nombre vienen del servidor (firma
            // numérica + UUID). Nunca el nombre original ni el NIT del
            // cliente. Defensa en profundidad: rechazar traversal aquí también.
            String carpetaLimpia = nombreCarpeta.replace("..", "").replace("\\", "").replace("/", "_");
            String nombreLimpio = FileValidation.sanitizarNombreParaMostrar(nombreArchivo)
                    .replace("/", "_").replace("\\", "_");
            if (nombreLimpio.isBlank() || nombreLimpio.contains("..")) {
                throw new IllegalArgumentException("Nombre de archivo no válido");
            }
            String rutaVirtual = carpetaLimpia + "/" + nombreLimpio;
            String urlFinal = supabaseUrl + rutaVirtual;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey.trim());
            headers.set("apikey", supabaseKey.trim());

            // Fallback seguro si el cliente no envía Content-Type
            String contentType = archivo.getContentType() != null
                    ? archivo.getContentType()
                    : "application/octet-stream";
            headers.setContentType(MediaType.parseMediaType(contentType));

            HttpEntity<byte[]> entity = new HttpEntity<>(archivo.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(urlFinal, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return rutaVirtual;
            } else {
                throw new RuntimeException("Respuesta inesperada de Supabase: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al subir a Supabase: " + e.getMessage());
        }
    }

    @Override
    public Resource cargarArchivo(String rutaRelativa) {
        try {
            FileValidation.validarRutaAlmacenada(rutaRelativa);
            String path = rutaRelativa.startsWith("/") ? rutaRelativa.substring(1) : rutaRelativa;
            String urlDescarga = supabaseUrl + path; // Cambio a variable dinámica

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey.trim()); // Cambio a variable dinámica
            headers.set("apikey", supabaseKey.trim());                    // Cambio a variable dinámica

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    urlDescarga,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ByteArrayResource(response.getBody());
            } else {
                throw new RuntimeException("No se pudo obtener el archivo. Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al recuperar archivo de Supabase: " + e.getMessage());
        }
    }

    @Override
    public void eliminarArchivo(String rutaRelativa) {
        try {
            FileValidation.validarRutaAlmacenada(rutaRelativa);
            String path = rutaRelativa.startsWith("/") ? rutaRelativa.substring(1) : rutaRelativa;
            String urlEliminar = supabaseUrl + path; // Cambio a variable dinámica

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey.trim()); // Cambio a variable dinámica
            headers.set("apikey", supabaseKey.trim());                    // Cambio a variable dinámica

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    urlEliminar,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase no pudo eliminar el archivo. Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el archivo físico en Supabase: " + e.getMessage());
        }
    }
}