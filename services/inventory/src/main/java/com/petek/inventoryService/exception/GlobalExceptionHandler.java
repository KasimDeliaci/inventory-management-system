package com.petek.inventoryService.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import com.petek.inventoryService.dto.ErrorResponse;

/**
 * Global exception translator that produces ErrorResponse JSON
 * matching the OpenAPI Error schema:
 *
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/v1/products",
 *   "timestamp": "2025-09-01T09:12:00Z",
 *   "details": {
 *     "invalidParams": ["categor_y"],
 *     "validationErrors": { "price_gte": "Must be >= 0" }
 *   }
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400: Handle validation errors (e.g. @Valid in DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
        MethodArgumentNotValidException ex,
        WebRequest request
    ) {
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(Instant.now())
                .details(validationErrors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    // 404: Handle resource not found
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResponseStatusException ex,
        WebRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message("Resource not found")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 409: Handle database constraint violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException ex,
        WebRequest request
    ) {
        String rootMessage = ex.getRootCause() != null
            ? ex.getRootCause().getMessage()
            : ex.getMessage();

        Map<String, String> details = new HashMap<>();

        if (rootMessage != null) {
            if (rootMessage.contains("chk_reorder_vs_safety")) details.put("chk_reorder_vs_safety", "Reorder must be >= Safety Stock");
            if (rootMessage.contains("chk_uom_allowed")) details.put("chk_uom_allowed", "Unit of Measure is not allowed");
            else details.put("database", rootMessage); // fallback to raw DB message
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("Database constraint violation")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // 500: Handle uncaught exceptions (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        Map<String, String> details = new HashMap<>();
        details.put(ex.getClass().getSimpleName(), ex.getMessage() != null ? ex.getMessage() : "");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

