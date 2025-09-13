package com.petek.inventoryService.dto.salesOrder;

import java.time.LocalDate;

import com.petek.inventoryService.entity.SalesOrder.SalesOrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderUpdateRequest {
    private LocalDate deliveryDate;
    private SalesOrderStatus status;
}
