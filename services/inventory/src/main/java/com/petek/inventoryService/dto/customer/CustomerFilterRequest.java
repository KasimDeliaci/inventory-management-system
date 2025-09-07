package com.petek.inventoryService.dto.customer;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.petek.inventoryService.entity.Customer.CustomerSegment;

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
public class CustomerFilterRequest {
    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("customerId");

    private String q;
    private CustomerSegment segment;
    private String city;

    @JsonProperty("updated_after")
    private Instant updatedAfter;
}
