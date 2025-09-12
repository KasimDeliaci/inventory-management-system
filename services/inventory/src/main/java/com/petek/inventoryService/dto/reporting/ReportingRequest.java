package com.petek.inventoryService.dto.reporting;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportingRequest {
    @NotNull
    private LocalDate from;

    @NotNull
    private LocalDate to;

    private List<Long> productId;    
}
