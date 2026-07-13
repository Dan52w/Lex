package com.example.lexcloud.service.impl;

import com.example.lexcloud.service.FileStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    // 1. Verifica que este ID coincida con tu proyecto actual de Supabase
    // La estructura es: .../storage/v1/object/[NOMBRE_DEL_BUCKET]/
    private final String SUPABASE_URL = "https://zcopsgolxkkytepcsghf.supabase.co/storage/v1/object/documentos/";

    // 2. Usa siempre la Service Role Key para operaciones de backend (la que ya tienes está bien)
    private final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inpjb3BzZ29seGtreXRlcGNzZ2hmIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3ODMzODcxNywiZXhwIjoyMDkzOTE0NzE3fQ.QcLMP0u8C4pX3ET0gpsSVf-iNsWJxgx1J2jtrnfOW-4";

    @Override
    public String guardarArchivo(MultipartFile archivo, String nombreCarpeta) {
        try {
            // Limpiamos el nombre del archivo para evitar caracteres raros
            String originalName = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename().replace(" ", "_") : "archivo";
            String nombreArchivo = System.currentTimeMillis() + "_" + originalName;

            String rutaVirtual = nombreCarpeta + "/" + nombreArchivo;
            String urlFinal = SUPABASE_URL + rutaVirtual;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_KEY.trim());
            headers.set("apikey", SUPABASE_KEY.trim());

            // Es vital enviar el Content-Type correcto para que Supabase sepa qué es (PDF, PNG, etc.)
            headers.setContentType(MediaType.parseMediaType(archivo.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(archivo.getBytes(), headers);

            // Supabase usa POST para subir archivos nuevos
            ResponseEntity<String> response = restTemplate.exchange(urlFinal, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return rutaVirtual; // Guardamos "nombre_carpeta/archivo.pdf" en la base de datos
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
            // Importante: Eliminar cualquier "/" extra al principio de la rutaRelativa
            String path = rutaRelativa.startsWith("/") ? rutaRelativa.substring(1) : rutaRelativa;
            String urlDescarga = SUPABASE_URL + path;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_KEY.trim());
            headers.set("apikey", SUPABASE_KEY.trim());

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
            // Limpiamos cualquier "/" extra al principio tal como en la descarga
            String path = rutaRelativa.startsWith("/") ? rutaRelativa.substring(1) : rutaRelativa;
            String urlEliminar = SUPABASE_URL + path;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_KEY.trim());
            headers.set("apikey", SUPABASE_KEY.trim());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Supabase requiere una petición DELETE para remover el objeto físico
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