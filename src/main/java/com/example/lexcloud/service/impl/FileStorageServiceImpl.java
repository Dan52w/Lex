package com.example.lexcloud.service.impl;

import com.example.lexcloud.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    // 1. Definimos la raíz base del proyecto.
    // Todos los archivos vivirán dentro de esta carpeta.
    private final Path root = Paths.get("uploads/documentos");

    @Override
    public String guardarArchivo(MultipartFile archivo, String nombreCarpeta) {
        try {
            // 2. Creamos la ruta física completa para guardar: uploads/documentos/firma_123
            Path rutaFirma = this.root.resolve(nombreCarpeta);

            if (!Files.exists(rutaFirma)) {
                Files.createDirectories(rutaFirma);
            }

            // 3. Generamos el nombre único del archivo
            String nombreArchivo = System.currentTimeMillis() + "_" + archivo.getOriginalFilename();

            // 4. Guardamos físicamente en el disco
            Files.copy(archivo.getInputStream(), rutaFirma.resolve(nombreArchivo));

            // 5. RETORNAMOS SOLO LA PARTE RELATIVA
            // En la BD se guardará: "firma_123/1712415_doc.pdf"
            // Usamos "/" explícitamente para que sea compatible entre Windows y Linux
            return nombreCarpeta + "/" + nombreArchivo;

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo: " + e.getMessage());
        }
    }

    @Override
    public Resource cargarArchivo(String rutaRelativa) {
        try {
            // 6. Al cargar, unimos la raíz con la ruta relativa de la BD
            // root (uploads/documentos) + rutaRelativa (firma_123/doc.pdf)
            Path archivoPath = root.resolve(rutaRelativa).normalize();

            Resource recurso = new UrlResource(archivoPath.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return recurso;
            } else {
                // Si falla, lanzamos la ruta absoluta para depurar en consola
                throw new RuntimeException("No existe el archivo en: " + archivoPath.toAbsolutePath());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL mal formada: " + e.getMessage());
        }
    }
}