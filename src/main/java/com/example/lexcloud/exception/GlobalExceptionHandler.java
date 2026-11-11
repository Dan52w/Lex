package com.example.lexcloud.exception;

import com.example.lexcloud.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Recurso no encontrado (404)
    // Mensaje FIJO: el detalle interno (qué recurso, qué id) se loguea pero
    // nunca se expone al cliente, para que "no existe" y "existe sin acceso"
    // sean indistinguibles.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Recurso no encontrado",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 2. Errores de validación de @Valid en los DTOs (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("timestamp", LocalDateTime.now());
        body.put("errores", errores);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 3. Reglas de negocio inválidas: argumentos incorrectos (400)
    // Ej: "El correo no es válido", "El id de tipo caso no existe" pasado como argumento suelto, etc.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Solicitud inválida: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 4. Conflictos de estado: "ya existe", duplicados (409)
    // Ej: TipoCasoServiceImpl, RolServiceImpl, EstadoServiceImpl usan esto para nombres duplicados
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflicto de estado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // 5. Violaciones de seguridad multi-tenant (403)
    // Ej: ClienteServiceImpl.actualizar/desactivarCliente, VinculoCasoServiceImpl.vincular
    // Mensaje FIJO: el detalle interno se loguea pero nunca se expone al cliente.
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "No tiene permisos para realizar esta operación",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 6. Acceso denegado por Spring Security (@PreAuthorize, method security) (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "No tiene permisos para realizar esta operación",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 7. Credenciales incorrectas en login (401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Correo o contraseña incorrectos",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 8. Parámetro de tipo incorrecto en la URL (400)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("El parámetro '%s' debe ser de tipo %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido");

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 9. Cualquier otro error no controlado (500)
    // IMPORTANTE: no se expone ex.getMessage() al cliente, solo se loguea internamente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrió un error inesperado. Intente nuevamente más tarde.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "El registro ya existe o viola una restricción de datos.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // 10. Ruta no existente (404) — p. ej. un endpoint que el front espera
    // y el backend aún no expone (fue el caso de GET /api/auth/me). Antes
    // caía en el catch-all y se logueaba como 500 "inesperado"; ahora
    // devuelve 404 limpio. Mensaje FIJO, como en el handler 1.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Ruta no encontrada: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Recurso no encontrado",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 11. Método HTTP no permitido para la ruta (405) — p. ej. GET a un
    // endpoint que solo acepta POST.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Método no permitido: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Método no permitido para este recurso",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 12. Cuerpo de la petición no legible (400) — JSON mal formado o
    // tipo de contenido incorrecto en un @RequestBody.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de la petición no legible: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "El cuerpo de la petición no es un JSON válido",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 13. Falta un parámetro requerido en la URL (400).
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = String.format("Falta el parámetro requerido '%s'", ex.getParameterName());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 14. Violaciones de @Validated en parámetros de método (400) — el
    // equivalente del handler 2 para validaciones fuera de los DTOs.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errores.put(v.getPropertyPath().toString(), v.getMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("timestamp", LocalDateTime.now());
        body.put("errores", errores);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 15. Archivo que excede el tamaño máximo permitido (413) — el tope
    // de subida de documentos. Sin este handler, el límite de multipart
    // caía en el catch-all como 500.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Archivo excede el tamaño máximo permitido");
        ErrorResponse error = new ErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "El archivo excede el tamaño máximo permitido",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }
}