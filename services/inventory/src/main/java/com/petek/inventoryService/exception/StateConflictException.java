package com.petek.inventoryService.exception;

/**
 * For PO state conflicts, preferred supplier rule violations, etc.
 * Examples:
 * - Trying to cancel a PO with received quantities > 0
 * - Trying to change unitPrice after any quantity received
 * - Trying to set multiple preferred suppliers for same product
 */
public class StateConflictException extends RuntimeException {
    private final String field; // optional: "status", "quantityReceived", "isPreferred"

    public StateConflictException(String message) { 
        this(message, null); 
    }

    public StateConflictException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() { 
        return field; 
    }
}
