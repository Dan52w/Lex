package com.example.lexcloud.service.impl;

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
    public String guardarArchivo(MultipartFile archivo, String nombreCarpeta) {
        try {
            // La carpeta y el nombre vienen del cliente (NIT y nombre original
            // del archivo): sin sanitizar, un '../' permitiría escribir fuera
            // de la carpeta de la firma (path traversal, OWASP A01).
            String carpetaSegura = sanitizarSegmento(nombreCarpeta);
            String originalName = archivo.getOriginalFilename() != null
                    ? sanitizarSegmento(archivo.getOriginalFilename().replace(" ", "_"))
                    : "archivo";
            String nombreArchivo = System.currentTimeMillis() + "_" + originalName;

            String rutaVirtual = carpetaSegura + "/" + nombreArchivo;
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
        // La ruta viene de la BD, pero se valida igual: un valor almacenado
        // con '../' no debe poder leer fuera del bucket.
        String path = normalizarRuta(rutaRelativa);
        try {
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
        String path = normalizarRuta(rutaRelativa);
        try {
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

    /**
     * Deja un segmento de ruta con solo caracteres seguros (letras, dígitos,
     * guion, guion bajo y punto). Cualquier otro carácter (incluidas '/' y
     * '\') se convierte en '_', y los puntos iniciales se eliminan para que
     * el resultado nunca sea "." ni "..".
     */
    private static String sanitizarSegmento(String valor) {
        if (valor == null || valor.isBlank()) {
            return "archivo";
        }
        String limpio = valor.replaceAll("[^A-Za-z0-9._-]", "_");
        limpio = limpio.replaceAll("^\\.+", "");
        if (limpio.isBlank() || limpio.equals(".") || limpio.equals("..")) {
            return "archivo";
        }
        return limpio;
    }

    /**
     * Valida una ruta relativa almacenada: sin segmentos "..", sin barras
     * invertidas ni caracteres de control, y sin ser absoluta.
     */
    private static String normalizarRuta(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía.");
        }
        String path = rutaRelativa.startsWith("/") ? rutaRelativa.substring(1) : rutaRelativa;
        for (String segmento : path.split("/")) {
            if (segmento.equals("..") || segmento.contains("\\")
                    || !segmento.chars().allMatch(c -> c >= 32 && c != 127)) {
                throw new IllegalArgumentException("La ruta del archivo no es válida.");
            }
        }
        return path;
    }
}