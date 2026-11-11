package com.example.lexcloud.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Tamaño máximo de subida: 10 MB (C9). Se aplica EN CÓDIGO, nunca en
     * properties. Es la ÚNICA definición del tope: la implementación, el
     * controlador, DocumentoServiceImpl y el bean MultipartConfig la
     * reutilizan (N10: antes vivía como static en la implementación, lo
     * que acoplaba a las capas superiores a la clase concreta).
     */
    long TAMANIO_MAXIMO_BYTES = 10_485_760L;

    String guardarArchivo(MultipartFile archivo, String nombreCarpeta);
    Resource cargarArchivo(String nombreArchivo);
    void eliminarArchivo(String rutaRelativa);

    /**
     * Valida un archivo antes de subirlo (vacío, tamaño, extensión,
     * Content-Type y firma de contenido). Método de instancia de la
     * interfaz (N10): el controlador lo llama por el bean inyectado, no
     * por la clase concreta.
     *
     * @throws IllegalArgumentException si el archivo no pasa alguna validación
     */
    void validarArchivo(MultipartFile archivo);
}
