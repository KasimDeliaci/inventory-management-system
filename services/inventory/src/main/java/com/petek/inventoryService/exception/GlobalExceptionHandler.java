package com.petek.inventoryService.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

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

    // 500: Handle uncaught exceptions (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put(ex.getClass().getSimpleName(), ex.getMessage() != null ? ex.getMessage() : "");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(Instant.now())
                .details(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

