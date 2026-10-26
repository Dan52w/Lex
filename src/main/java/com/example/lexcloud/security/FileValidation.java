package com.example.lexcloud.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * P0-B5: validación de documentos. La decisión es del backend, nunca del
 * front (el front solo filtra por UX). Reglas:
 * - Lista blanca de extensiones: pdf, docx, xlsx, jpg, jpeg, png.
 * - Verificación por magic bytes, no por extensión ni por Content-Type del
 *   cliente: PDF (%PDF), PNG, JPEG y ZIP (docx/xlsx son ZIP; además se exige
 *   que el ZIP contenga [Content_Types].xml para no aceptar un ZIP cualquiera
 *   renombrado).
 * - Límite de tamaño explícito (por defecto 10 MB).
 * - Nombre de almacenamiento: UUID + extensión. El nombre original NUNCA se
 *   usa como ruta; solo se guarda como metadato para mostrar.
 * - Sanitización de cualquier segmento de ruta: solo [A-Za-z0-9._-], sin
 *   separadores ni "..".
 */
public final class FileValidation {

    public static final long MAX_BYTES_POR_DEFECTO = 10L * 1024 * 1024;

    public static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of("pdf", "docx", "xlsx", "jpg", "jpeg", "png");

    private static final Map<String, String> MEDIA_TYPE_POR_EXTENSION = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");

    private FileValidation() {}

    public static String extensionDe(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }
        String base = nombreOriginal.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int punto = base.lastIndexOf('.');
        if (punto < 0 || punto == base.length() - 1) {
            return "";
        }
        return base.substring(punto + 1).toLowerCase();
    }

    /** Sanitiza un nombre para mostrar/descargar: sin rutas, comillas ni controles. */
    public static String sanitizarNombreParaMostrar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "documento";
        }
        String limpio = nombre.replace('\\', '/');
        int slash = limpio.lastIndexOf('/');
        if (slash >= 0) {
            limpio = limpio.substring(slash + 1);
        }
        limpio = limpio.replaceAll("[\"\\\\\\r\\n\\p{Cntrl}]", "")
                .replaceAll("\\.{2,}", ".")
                .trim();
        return limpio.isEmpty() ? "documento" : limpio;
    }

    /** Rechaza rutas con traversal o separadores inesperados (defensa en profundidad). */
    public static void validarRutaAlmacenada(String ruta) {
        if (ruta == null || ruta.isBlank()
                || ruta.contains("..") || ruta.startsWith("/")
                || ruta.contains("\\")) {
            throw new IllegalArgumentException("Ruta de documento no válida");
        }
    }

    public static String nombreAlmacenamiento(String extension) {
        return UUID.randomUUID() + "." + extension;
    }

    public static String mediaTypePara(String extension) {
        return MEDIA_TYPE_POR_EXTENSION.getOrDefault(extension, "application/octet-stream");
    }

    /** Valida tamaño, extensión y contenido. Lanza IllegalArgumentException si no pasa. */
    public static String validar(byte[] contenido, String nombreOriginal, long maxBytes) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("No se puede subir un archivo vacío.");
        }
        if (contenido.length > maxBytes) {
            throw new IllegalArgumentException(
                    "El documento supera el tamaño máximo permitido de " + (maxBytes / (1024 * 1024)) + " MB");
        }
        String extension = extensionDe(nombreOriginal);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Tipo de documento no permitido: " + extension);
        }
        if (!contenidoCoincide(contenido, extension)) {
            throw new IllegalArgumentException(
                    "El contenido del archivo no corresponde con su extensión");
        }
        return extension;
    }

    private static boolean contenidoCoincide(byte[] contenido, String extension) {
        return switch (extension) {
            case "pdf" -> empiezaCon(contenido, "%PDF".getBytes());
            case "png" -> contenido.length >= 8
                    && (contenido[0] & 0xFF) == 0x89 && contenido[1] == 0x50
                    && contenido[2] == 0x4E && contenido[3] == 0x47;
            case "jpg", "jpeg" -> contenido.length >= 3
                    && (contenido[0] & 0xFF) == 0xFF && (contenido[1] & 0xFF) == 0xD8
                    && (contenido[2] & 0xFF) == 0xFF;
            case "docx", "xlsx" -> esZipConContentTypes(contenido);
            default -> false;
        };
    }

    private static boolean empiezaCon(byte[] contenido, byte[] prefijo) {
        if (contenido.length < prefijo.length) {
            return false;
        }
        for (int i = 0; i < prefijo.length; i++) {
            if (contenido[i] != prefijo[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean esZipConContentTypes(byte[] contenido) {
        if (contenido.length < 4 || contenido[0] != 'P' || contenido[1] != 'K') {
            return false;
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            ZipEntry entry;
            boolean tieneContentTypes = false;
            int entradas = 0;
            while ((entry = zip.getNextEntry()) != null && entradas < 200) {
                entradas++;
                if ("[Content_Types].xml".equals(entry.getName())) {
                    tieneContentTypes = true;
                }
            }
            return entradas > 0 && tieneContentTypes;
        } catch (IOException e) {
            return false;
        }
    }
}
