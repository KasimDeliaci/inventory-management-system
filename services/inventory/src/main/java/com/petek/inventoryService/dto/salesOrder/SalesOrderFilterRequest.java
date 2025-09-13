package com.petek.inventoryService.dto.salesOrder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.petek.inventoryService.entity.SalesOrder.SalesOrderStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderFilterRequest {
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("salesOrderId");
    
    private List<Integer> customerId;
    private SalesOrderStatus status;
    private LocalDate orderDateGte;
    private LocalDate orderDateLte;
    private LocalDate deliveryDateGte;
    private LocalDate deliveryDateLte;
    private Instant deliveredSince;
    private Instant updatedAfter;
}