package com.petek.inventoryService.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // === 400: Body bean validation (@Valid on request DTO) ==================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> validation = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> validation.put(fe.getField(), friendlyMessage(fe)));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> validation.put(ge.getObjectName(), ge.getDefaultMessage()));

        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, d -> {
            d.validationErrors.putAll(validation);
        });
    }

    // === 400: @Validated on query params/path variables =====================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {

        Map<String, String> validation = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String name = lastPathNode(v.getPropertyPath().toString());
            validation.put(name, v.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, d -> {
            d.validationErrors.putAll(validation);
        });
    }

    // === 400: Binding errors (query/form) ==================================
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex, HttpServletRequest req) {

        Map<String, String> validation = new LinkedHashMap<>();
        for (FieldError fe : ex.getFieldErrors()) {
            validation.put(fe.getField(), friendlyMessage(fe));
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, d -> {
            d.validationErrors.putAll(validation);
        });
    }

    // === 400: Missing required query param =================================
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {

        return build(HttpStatus.BAD_REQUEST, "Missing required parameter", req, d -> {
            d.invalidParams.add(ex.getParameterName());
            d.validationErrors.put(ex.getParameterName(), "Parameter is required");
        });
    }

    // === 400: JSON parse errors / unknown body fields =======================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {

        String msg = rootMessage(ex);
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req, d -> {
            if (msg != null) d.validationErrors.put("_body", msg);
        });
    }

    // === 400: Type mismatch in query/path ==================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {

        String name = ex.getName();
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "type";
        String message = "Expected " + expected + " but got '" + String.valueOf(ex.getValue()) + "'";
        return build(HttpStatus.BAD_REQUEST, "Invalid parameter type", req, d -> {
            d.validationErrors.put(name, message);
        });
    }

    // === 400: Unknown/Disallowed query parameters (your strict rule) =======
    @ExceptionHandler(UnknownQueryParamsException.class)
    public ResponseEntity<ErrorResponse> handleUnknownParams(
            UnknownQueryParamsException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Unknown query parameter(s)", req, d -> {
            d.invalidParams.addAll(ex.getInvalidParams());
        });
    }

    // === 404: Not Found ====================================================
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResponseStatusException ex,
        HttpServletRequest req
    ) {
        return build(HttpStatus.NOT_FOUND, ex.getReason(), req, d -> { });
    }

    // === 409: Business/state conflicts (cancel with receipts, etc.) =========
    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<ErrorResponse> handleStateConflict(
            StateConflictException ex, HttpServletRequest req) {

        return build(HttpStatus.CONFLICT, ex.getMessage(), req, d -> {
            // Optionally expose a field-specific hint:
            if (ex.getField() != null) {
                d.validationErrors.put(ex.getField(), ex.getMessage());
            }
        });
    }

    // === 409: Overlap conflicts (campaign/product or customer offer) ========
    @ExceptionHandler(OverlapConflictException.class)
    public ResponseEntity<ErrorResponse> handleOverlapConflict(
            OverlapConflictException ex, HttpServletRequest req) {

        return build(HttpStatus.CONFLICT, ex.getMessage(), req, d -> {
            if (ex.getConflictingId() != null) {
                d.validationErrors.put("conflictWithId", ex.getConflictingId().toString());
            }
        });
    }

    // === 409: Uniqueness / FK constraint violations from DB =================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {

        String constraint = extractConstraintName(ex);
        String message = switch (StringUtils.hasText(constraint) ? constraint : "") {
            case "uq_product_supplier"        -> "Product is already linked to this supplier";
            case "uq_prod_one_preferred"      -> "Only one preferred supplier is allowed per product";
            case "uq_campaign_product"        -> "Product is already assigned to this campaign";
            case "uq_customer_email"          -> "Email is already in use";
            case "uq_supplier_email"          -> "Email is already in use";
            case "uq_customer_special_offer"  -> "Customer already has an active special offer";
            case "uq_po_product"              -> "Product is already in this purchase order";
            default                           -> "Conflict with existing data";
        };
        return build(HttpStatus.CONFLICT, message, req, d -> {
            if (StringUtils.hasText(constraint)) {
                d.validationErrors.put("constraint", constraint);
            }
        });
    }

    // === 404/405 (optional—but nice) =======================================
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(
            NoHandlerFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", req, d -> { });
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", req, d -> { });
    }

    // === 400: Generic invalid input guard ==================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, d -> { });
    }

    // === 500: Fallback ======================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(
            Exception ex, HttpServletRequest req) {
        // Log the exception for debugging in production
        System.err.println("Unhandled exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req, d -> {
            // In production, avoid echoing exception details. Log it instead.
        });
    }

    // ======= helpers ========================================================

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest req,
            java.util.function.Consumer<ErrorDetails> detailsCustomizer
    ) {

        ErrorDetails details = new ErrorDetails();
        if (detailsCustomizer != null) detailsCustomizer.accept(details);

        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                details.isEmpty() ? null : details
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(body, headers, status);
    }

    private static String friendlyMessage(FieldError fe) {
        String msg = fe.getDefaultMessage();
        if (!StringUtils.hasText(msg)) {
            msg = "Invalid value";
        }
        return msg;
    }

    private static String lastPathNode(String path) {
        if (path == null) return "";
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage();
    }

    // Try to pull constraint/index name out of the SQL exception chain
    private static final Pattern CONSTRAINT_RX =
            Pattern.compile("constraint [\"'`]?([\\w_]+)[\"'`]?", Pattern.CASE_INSENSITIVE);

    private static String extractConstraintName(DataIntegrityViolationException ex) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof SQLException sqlEx) {
                String msg = sqlEx.getMessage();
                if (msg != null) {
                    Matcher m = CONSTRAINT_RX.matcher(msg);
                    if (m.find()) return m.group(1);
                }
            }
            cur = cur.getCause();
        }
        return null;
    }

    // ========= Error schema (matches your OpenAPI) ==========================

    public static final class ErrorResponse {
        public final int status;
        public final String error;
        public final String message;
        public final String path;
        public final OffsetDateTime timestamp;
        public final ErrorDetails details;

        public ErrorResponse(int status, String error, String message, String path,
                             OffsetDateTime timestamp, ErrorDetails details) {
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.timestamp = timestamp;
            this.details = details;
        }

        // Getters for JSON serialization
        public int getStatus() { return status; }
        public String getError() { return error; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public OffsetDateTime getTimestamp() { return timestamp; }
        public ErrorDetails getDetails() { return details; }
    }

    public static final class ErrorDetails {
        public final List<String> invalidParams = new ArrayList<>();
        public final Map<String, String> validationErrors = new LinkedHashMap<>();
        
        boolean isEmpty() {
            return invalidParams.isEmpty() && validationErrors.isEmpty();
        }

        // Getters for JSON serialization
        public List<String> getInvalidParams() { return invalidParams; }
        public Map<String, String> getValidationErrors() { return validationErrors; }
    }
}
