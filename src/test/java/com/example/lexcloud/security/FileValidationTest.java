package com.example.lexcloud.security;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P0-A4 prueba 10: subir ejecutable, HTML con script o archivo con "../"
 * es rechazado o neutralizado (nombre UUID + sanitización).
 */
class FileValidationTest {

    @Test
    void pdfValidoPasa() {
        byte[] pdf = "%PDF-1.7 contenido falso".getBytes(StandardCharsets.UTF_8);
        assertEquals("pdf", FileValidation.validar(pdf, "informe.pdf", 10_000_000));
    }

    @Test
    void ejecutableRechazadoAunqueSeLlamePdf() {
        byte[] exe = "MZ contenido de ejecutable".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validar(exe, "informe.pdf", 10_000_000));
    }

    @Test
    void htmlConScriptRechazado() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validar(html, "pagina.html", 10_000_000));
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validar(html, "pagina.pdf", 10_000_000));
    }

    @Test
    void extensionNoPermitidaRechazada() {
        byte[] pdf = "%PDF-1.7 x".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validar(pdf, "archivo.exe", 10_000_000));
    }

    @Test
    void tamanoMayorAlLimiteRechazado() {
        byte[] pdf = "%PDF-1.7 x".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validar(pdf, "a.pdf", 5));
    }

    @Test
    void zipSinContentTypesNoPasaComoDocx() {
        byte[] zip = zipConEntradas("word/document.xml");
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validar(zip, "doc.docx", 10_000_000));
    }

    @Test
    void docxValidoPasa() {
        byte[] zip = zipConEntradas("[Content_Types].xml", "word/document.xml");
        assertEquals("docx", FileValidation.validar(zip, "doc.docx", 10_000_000));
    }

    @Test
    void nombreConTraversalSeNeutraliza() {
        // La extensión se extrae del nombre base, no de la ruta completa.
        assertEquals("pdf", FileValidation.extensionDe("../../secreto.pdf"));
        String limpio = FileValidation.sanitizarNombreParaMostrar("../../secreto.pdf");
        assertFalse(limpio.contains("/"), "El nombre para mostrar no debe tener rutas");
        assertFalse(limpio.contains(".."), "El nombre para mostrar no debe tener ..");
        String almacenamiento = FileValidation.nombreAlmacenamiento("pdf");
        assertTrue(almacenamiento.matches("[0-9a-f-]+\\.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validarRutaAlmacenada("../firma_1/archivo.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validarRutaAlmacenada("/absoluto/archivo.pdf"));
    }

    @Test
    void pngYJpgPorMagicBytes() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
        assertEquals("png", FileValidation.validar(png, "foto.png", 10_000_000));
        byte[] jpg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 1};
        assertEquals("jpg", FileValidation.validar(jpg, "foto.jpg", 10_000_000));
        // Un PNG renombrado a .jpg no pasa.
        assertThrows(IllegalArgumentException.class,
                () -> FileValidation.validar(png, "foto.jpg", 10_000_000));
    }

    private byte[] zipConEntradas(String... nombres) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (String nombre : nombres) {
                    zip.putNextEntry(new ZipEntry(nombre));
                    zip.write("contenido".getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
