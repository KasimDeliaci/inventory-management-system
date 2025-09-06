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

import jakarta.persistence.EntityNotFoundException;

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
 *     "price_gte": "Must be >= 0"
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

    // 400: Handle illegal arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
        IllegalArgumentException ex,
        WebRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(Instant.now())
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

    // 404: Handle resource not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
        EntityNotFoundException ex,
        WebRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
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

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message("Database constraint violation")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(Instant.now())
                .details(getConstraintMessage(rootMessage))
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

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.ofEntries(
        // Products
        Map.entry("chk_product_name_nonblank", "Product name cannot be empty or whitespace"),
        Map.entry("chk_category_nonblank", "Product category cannot be empty or whitespace"),
        Map.entry("chk_uom_allowed", "Unit of measure must be one of: adet, ton, kg, g, lt, ml, koli, paket, çuval, şişe"),
        Map.entry("chk_safety_nonneg", "Safety stock cannot be negative"),
        Map.entry("chk_reorder_nonneg", "Reorder point cannot be negative"),
        Map.entry("chk_reorder_vs_safety", "Reorder point must be greater than or equal to safety stock"),
        Map.entry("chk_price_nonneg", "Product price cannot be negative"),
        
        // Suppliers
        Map.entry("chk_supplier_name_nonblank", "Supplier name cannot be empty or whitespace"),
        Map.entry("chk_supplier_city_nonblank", "Supplier city cannot be empty or whitespace"),
        Map.entry("uq_suppliers_email_active", "This email is already registered for another active supplier"),
        
        // Customers
        Map.entry("chk_customer_name_nonblank", "Customer name cannot be empty or whitespace"),
        Map.entry("chk_customer_city_nonblank", "Customer city cannot be empty or whitespace"),
        Map.entry("uq_customers_email_active", "This email is already registered for another active customer"),
        
        // Product Suppliers
        Map.entry("min_order_quantity", "Minimum order quantity must be greater than 0"),
        Map.entry("avg_lead_time_days", "Average lead time cannot be negative"),
        Map.entry("avg_delay_days", "Average delay days cannot be negative"),
        Map.entry("chk_total_orders_nonneg", "Total orders count cannot be negative"),
        Map.entry("chk_delayed_orders_nonneg", "Delayed orders count cannot be negative"),
        Map.entry("chk_orders_counts_consistent", "Delayed orders cannot exceed total orders count"),
        Map.entry("uq_product_supplier", "This product-supplier relationship already exists"),
        Map.entry("uq_prod_one_preferred", "A product can have only one preferred supplier"),
        
        // Campaign
        Map.entry("discount_percentage", "Discount percentage must be between 0 and 100"),
        Map.entry("buy_qty", "Buy quantity must be greater than 0"),
        Map.entry("get_qty", "Get quantity must be greater than 0"),
        Map.entry("chk_campaign_dates", "Campaign end date must be on or after start date"),
        Map.entry("chk_params_for_percent_off", "Discount campaigns require discount percentage and no buy/get quantities"),
        Map.entry("chk_params_for_bxgy", "Buy X Get Y campaigns require buy and get quantities, no discount percentage"),
        Map.entry("percent_off", "Percent off must be between 0 and 100"),
        Map.entry("chk_cso_dates", "Special offer end date must be on or after start date"),
        
        // Purchase Order
        Map.entry("chk_po_dates", "Purchase order dates must be logical (delivery after order date)"),
        Map.entry("chk_po_eta_required_in_transit", "Expected delivery date is required for in-transit orders"),
        Map.entry("chk_po_eta_after_received_forbidden", "Expected delivery date must be before or equal to actual delivery date for received orders"),
        Map.entry("chk_po_actual_on_received", "Actual delivery timestamp is required for received orders"),
        
        // Purchase Order Items
        Map.entry("quantity_ordered", "Ordered quantity must be greater than 0"),
        Map.entry("quantity_received", "Received quantity cannot exceed ordered quantity or be negative"),
        Map.entry("unit_price", "Unit price cannot be negative"),
        Map.entry("uq_poi_unique_product_per_po", "Each product can only appear once per purchase order"),
        
        // Sales Order
        Map.entry("customer_discount_pct_applied", "Customer discount percentage must be between 0 and 100"),
        Map.entry("chk_so_dates", "Sales order dates must be logical (delivery after order date)"),
        Map.entry("chk_so_delivered_pair", "Delivered timestamp is required for delivered orders only"),
        
        // Sales Order Items
        Map.entry("quantity", "Quantity must be greater than 0"),
        Map.entry("uq_soi_unique_product_per_so", "Each product can only appear once per sales order"),
        
        // Stock Movement
        Map.entry("chk_source_required", "Purchase/sales orders require source IDs, adjustments must not have source IDs"),
        Map.entry("chk_kind_by_source", "Movement kind must match movement source (e.g., purchase receipts from purchase orders only)"),
        Map.entry("uq_sm_one_per_source_line", "This order line has already been processed"),
        
        // Current Stock
        Map.entry("quantity_on_hand", "Quantity on hand cannot be negative"),
        Map.entry("quantity_reserved", "Reserved quantity cannot be negative"),
        Map.entry("chk_available_nonneg", "Available quantity cannot be negative (on hand must be >= reserved)"),
        
        // Foreign key constraints
        Map.entry("foreign_key", "Referenced record does not exist or cannot be deleted due to existing references")
    );

    public static Map<String, String> getConstraintMessage(String rootMessage) {
    return CONSTRAINT_MESSAGES.entrySet().stream()
        .filter(entry -> rootMessage.contains(entry.getKey()))
        .findFirst()
        .map(entry -> Map.of(entry.getKey(), entry.getValue()))
        .orElse(Map.of("database", rootMessage)); // fallback to raw message
}

}

