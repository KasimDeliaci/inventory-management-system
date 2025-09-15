package com.petek.planning.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastRequest {
    private List<Long> productIds;
    private Integer horizonDays;
    private String asOfDate;
    private Boolean returnDaily;
}
