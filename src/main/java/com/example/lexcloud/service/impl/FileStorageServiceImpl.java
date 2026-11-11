package com.example.lexcloud.service.impl;

import com.example.lexcloud.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Almacenamiento de archivos en Supabase Storage.
 *
 * <p><b>Límite transaccional (A11):</b> el storage NO participa en ninguna
 * transacción de base de datos. Subir un archivo y guardar su registro en BD
 * son dos operaciones independientes: si el guardado en BD falla después de
 * la subida, el controlador compensa borrando el archivo (C9). Este servicio
 * no promete atomicidad entre storage y BD.</p>
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    /** Extensiones permitidas (C9). Única definición; la reutilizan ambas capas. */
    public static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");

    /** Content-Type esperado para cada extensión permitida (C9). */
    public static final Map<String, String> CONTENT_TYPE_POR_EXTENSION = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");

    /**
     * Firmas de contenido (magic bytes) por extensión (S9). Un archivo
     * Office Open XML (docx/xlsx) es un ZIP: acepta la firma de entrada
     * de archivo ({@code PK\x03\x04}) y la de ZIP vacío ({@code PK\x05\x06}).
     * Los formatos OLE2 (doc/xls) comparten la firma de Compound File.
     */
    private static final Map<String, byte[][]> FIRMAS_POR_EXTENSION = Map.of(
            "pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}, // %PDF
            "jpg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
            "jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
            "png", new byte[][]{{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}},
            "doc", new byte[][]{{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}},
            "xls", new byte[][]{{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}},
            "docx", new byte[][]{{0x50, 0x4B, 0x03, 0x04}, {0x50, 0x4B, 0x05, 0x06}},
            "xlsx", new byte[][]{{0x50, 0x4B, 0x03, 0x04}, {0x50, 0x4B, 0x05, 0x06}});

    /**
     * Longitud máxima del nombre (ya sanitizado, con extensión) que se
     * conserva del archivo original (B6). El nombre almacenado final es
     * {@code UUID + "_" + nombre}, así que el total queda acotado.
     */
    public static final int LONGITUD_MAXIMA_NOMBRE_ARCHIVO = 80;

    private final RestTemplate restTemplate;

    // Se inyectan de manera dinámica desde el application.properties
    @Value("${supabase.storage.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.key}")
    private String supabaseKey;

    /**
     * A13: se inyecta el bean único de {@code HttpClientConfig} (con timeouts);
     * nunca se crea un {@code RestTemplate} por llamada.
     */
    public FileStorageServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Valida un archivo ANTES de subirlo (C9): no vacío, tamaño dentro del
     * tope, extensión permitida, Content-Type coherente con la extensión y
     * firma de contenido (magic bytes, S9) coherente con la extensión.
     * La usa el controlador (validación temprana) y la re-valida
     * {@link #guardarArchivo(MultipartFile, String)} (defensa en profundidad:
     * no se confía solo en el controlador).
     *
     * <p>El Content-Type lo declara el cliente, así que solo se comprueba su
     * coherencia con la extensión; si no viene declarado, manda la extensión.
     * La firma de contenido sí se inspecciona: la extensión y el
     * Content-Type se pueden falsificar sin cambiar el archivo, la cabecera
     * no. La verificación lee los primeros bytes (el archivo ya está acotado
     * a 10 MB y {@code guardarArchivo} lo vuelve a leer para subirlo).</p>
     *
     * @throws IllegalArgumentException si el archivo no pasa alguna validación
     */
    @Override
    public void validarArchivo(MultipartFile archivo) {
        if (archivo == null) {
            throw new IllegalArgumentException("El archivo es obligatorio.");
        }
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("No se puede subir un archivo vacío.");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido de 10 MB.");
        }
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre.");
        }
        String extension = extraerExtension(nombreOriginal);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("El tipo de archivo no está permitido.");
        }
        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String esperado = CONTENT_TYPE_POR_EXTENSION.get(extension);
            if (!esperado.equalsIgnoreCase(contentType.trim())) {
                throw new IllegalArgumentException("El Content-Type no corresponde con la extensión del archivo.");
            }
        }
        verificarFirmaContenido(archivo, extension);
    }

    /**
     * Comprueba que la cabecera del archivo coincida con una de las firmas
     * conocidas de su extensión (S9). Un archivo más corto que la firma, o
     * que no empiece por ninguna, se rechaza con 400 claro.
     */
    private static void verificarFirmaContenido(MultipartFile archivo, String extension) {
        byte[][] firmas = FIRMAS_POR_EXTENSION.get(extension);
        if (firmas == null) {
            return; // Sin firma conocida para la extensión; no se bloquea.
        }
        final byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo.");
        }
        for (byte[] firma : firmas) {
            if (contenido.length >= firma.length && comienzaCon(contenido, firma)) {
                return;
            }
        }
        throw new IllegalArgumentException("El contenido del archivo no corresponde con su extensión.");
    }

    private static boolean comienzaCon(byte[] contenido, byte[] firma) {
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extraerExtension(String nombreArchivo) {
        int lastDot = nombreArchivo.lastIndexOf('.');
        if (lastDot < 0 || lastDot == nombreArchivo.length() - 1) {
            return "";
        }
        return nombreArchivo.substring(lastDot + 1).toLowerCase();
    }

    @Override
    public String guardarArchivo(MultipartFile archivo, String nombreCarpeta) {
        // C9: re-validación en esta capa (defensa en profundidad).
        validarArchivo(archivo);
        if (nombreCarpeta == null || nombreCarpeta.isBlank()) {
            throw new IllegalArgumentException("La carpeta de destino es obligatoria.");
        }
        try {
            // La carpeta y el nombre vienen del cliente (NIT y nombre original
            // del archivo): sin sanitizar, un '../' permitiría escribir fuera
            // de la carpeta de la firma (path traversal, OWASP A01).
            String carpetaSegura = sanitizarSegmento(nombreCarpeta);
            String originalName = archivo.getOriginalFilename() != null
                    ? sanitizarSegmento(archivo.getOriginalFilename().replace(" ", "_"))
                    : "archivo";
            // A10: el nombre almacenado lleva un UUID (nunca el nombre original
            // tal cual: colisiones + traversal). B6: el nombre se trunca a una
            // longitud segura conservando la extensión.
            String nombreArchivo = UUID.randomUUID() + "_" + truncarNombre(originalName);

            String rutaVirtual = carpetaSegura + "/" + nombreArchivo;
            String urlFinal = supabaseUrl + rutaVirtual;

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

        } catch (HttpClientErrorException.NotFound e) {
            // Pendiente #3 de controllers: un documento cuyo archivo ya no
            // está en Supabase responde 404, no 500. RestTemplate lanza
            // NotFound antes de llegar a la rama no-2xx, así que este catch
            // debe ir ANTES del genérico.
            throw new EntityNotFoundException("Recurso no encontrado");
        } catch (Exception e) {
            throw new RuntimeException("Error al recuperar archivo de Supabase: " + e.getMessage());
        }
    }

    @Override
    public void eliminarArchivo(String rutaRelativa) {
        String path = normalizarRuta(rutaRelativa);
        try {
            String urlEliminar = supabaseUrl + path; // Cambio a variable dinámica

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
     * Trunca un nombre ya sanitizado a {@link #LONGITUD_MAXIMA_NOMBRE_ARCHIVO}
     * caracteres, conservando la extensión (B6). Un nombre sin extensión se
     * corta tal cual.
     */
    private static String truncarNombre(String nombreLimpio) {
        if (nombreLimpio.length() <= LONGITUD_MAXIMA_NOMBRE_ARCHIVO) {
            return nombreLimpio;
        }
        int dot = nombreLimpio.lastIndexOf('.');
        String extension = dot > 0 ? nombreLimpio.substring(dot) : "";
        String stem = dot > 0 ? nombreLimpio.substring(0, dot) : nombreLimpio;
        int maxStem = LONGITUD_MAXIMA_NOMBRE_ARCHIVO - extension.length();
        if (maxStem <= 0) {
            return nombreLimpio.substring(0, LONGITUD_MAXIMA_NOMBRE_ARCHIVO);
        }
        return stem.substring(0, Math.min(stem.length(), maxStem)) + extension;
    }

    /**
     * Valida una ruta relativa almacenada (M23, estricta).
     *
     * <p>Se RECHAZA (no se normaliza silenciosamente): rutas absolutas
     * (empiezan por '/'), separadores que escapan del directorio base
     * ('..', '.', segmentos vacíos de '//', '\', ':'), caracteres de control
     * y secuencias codificadas ('%', que cubriría '%2F', '?' y '#'). La ruta
     * es siempre relativa al bucket de Supabase (el directorio base): una
     * ruta que pasara esta validación no puede salir de él.</p>
     */
    private static String normalizarRuta(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía.");
        }
        if (rutaRelativa.startsWith("/") || rutaRelativa.contains("\\")
                || rutaRelativa.contains("%") || rutaRelativa.contains("?")
                || rutaRelativa.contains("#") || rutaRelativa.contains(":")) {
            throw new IllegalArgumentException("La ruta del archivo no es válida.");
        }
        for (String segmento : rutaRelativa.split("/", -1)) {
            if (segmento.isEmpty() || segmento.equals(".") || segmento.equals("..")
                    || !segmento.chars().allMatch(c -> c >= 32 && c != 127)) {
                throw new IllegalArgumentException("La ruta del archivo no es válida.");
            }
        }
        return rutaRelativa;
    }
}
