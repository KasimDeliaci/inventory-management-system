package com.petek.inventoryService.dto.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignUpdateRequest {
    @NotNull(message = "Campaign name is required")
    @Size(min = 2, max = 200, message = "Campaign name must be between 2 and 200 characters")
    private String campaignName;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount percentage must be between 0 and 100")
    @DecimalMax(value = "100.0", inclusive = true, message = "Discount percentage must be between 0 and 100")
    private BigDecimal discountPercentage;

    @Positive(message = "Buy quantity should be positive")
    private Integer buyQty;

    @Positive(message = "Get quantity should be positive")
    private Integer getQty;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
