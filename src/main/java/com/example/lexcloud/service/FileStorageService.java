package com.example.lexcloud.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String guardarArchivo(MultipartFile archivo, String nombreCarpeta);
    Resource cargarArchivo(String nombreArchivo);
}
