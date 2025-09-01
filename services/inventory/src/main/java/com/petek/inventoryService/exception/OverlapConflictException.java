package com.petek.inventoryService.exception;

/**
 * For overlapping campaign/product assignments or customer offer windows.
 * Examples:
 * - Product already assigned to another active campaign in overlapping time window
 * - Customer already has an active special offer in overlapping time window
 */
public class OverlapConflictException extends RuntimeException {
    private final Long conflictingId; // optional: the other row's id that conflicts

    public OverlapConflictException(String message) { 
        this(message, null); 
    }

    public OverlapConflictException(String message, Long conflictingId) {
        super(message);
        this.conflictingId = conflictingId;
    }

    public Long getConflictingId() { 
        return conflictingId; 
    }
}
