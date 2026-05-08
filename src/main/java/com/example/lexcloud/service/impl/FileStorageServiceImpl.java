package com.example.lexcloud.service.impl;

import com.example.lexcloud.service.FileStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final String SUPABASE_URL = "https://xjjlfqkobikvezkpaqnl.supabase.co/storage/v1/object/documentos/";
    private final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhqamxmcWtvYmlrdmV6a3BhcW5sIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3ODE2MDQyMywiZXhwIjoyMDkzNzM2NDIzfQ.30lZtylOpiuFh1pOPT57Xs64_XAd7faXl70epyc91cg";

    @Override
    public String guardarArchivo(MultipartFile archivo, String nombreCarpeta) {
        try {
            // 1. Generamos el nombre del archivo
            String nombreArchivo = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();

            // 2. Definimos la RUTA VIRTUAL: "firma_123/archivo.pdf"
            // Al enviarlo así, Supabase entiende que debe crear la "carpeta" firma_123
            String rutaVirtual = nombreCarpeta + "/" + nombreArchivo;
            String urlFinal = SUPABASE_URL + rutaVirtual;

            // 3. Preparamos la petición HTTP
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_KEY);
            headers.set("apikey", SUPABASE_KEY.trim());
            headers.setContentType(MediaType.parseMediaType(archivo.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(archivo.getBytes(), headers);

            // 4. Enviamos el archivo a Supabase
            restTemplate.exchange(urlFinal, HttpMethod.POST, entity, String.class);

            // 5. Retornamos la ruta que guardaremos en la BD (igual que antes)
            return rutaVirtual;

        } catch (Exception e) {
            throw new RuntimeException("Error al subir a Supabase: " + e.getMessage());
        }
    }

    @Override
    public Resource cargarArchivo(String rutaRelativa) {
        try {
            String urlDescarga = SUPABASE_URL + rutaRelativa;

            // Necesitamos descargar el archivo con los headers de seguridad
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_KEY.trim());
            headers.set("apikey", SUPABASE_KEY.trim());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Obtenemos los bytes del archivo
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    urlDescarga,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Devolvemos el contenido como un recurso en memoria
                return new ByteArrayResource(response.getBody());
            } else {
                throw new RuntimeException("No se pudo obtener el archivo de Supabase: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al recuperar archivo de Supabase: " + e.getMessage());
        }
    }
}