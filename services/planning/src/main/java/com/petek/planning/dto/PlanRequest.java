package com.petek.planning.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {    
    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be a positive number")
    private Long productId;
    
    @NotNull(message = "As of date is required")
    @PastOrPresent(message = "As of date cannot be in the future")
    private LocalDate asOfDate;
    
    @Pattern(regexp = "^(1|7|14)$", message = "Horizon days must be 1, 7, or 14")
    @Builder.Default
    private String horizonDays = "7";

    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Model name can only contain letters, numbers, underscores, and hyphens")
    private String model;
}
