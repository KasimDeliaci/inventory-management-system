package com.petek.planning.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petek.planning.dto.ForecastRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebClientService {
    
    private final WebClient webClient;

    public String getProduct(Long productId) {
        return webClient.get()
            .uri("http://localhost:8000/api/v1/products/" + productId)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    public String getSuppliers(Long productId) {
        return webClient.get()
            .uri("http://localhost:8000/api/v1/products/" + productId + "/suppliers")
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    public String getForecasts(Long productId, int horizonDays, LocalDate asOfDate) {
        ForecastRequest request = ForecastRequest.builder()
            .productIds(List.of(productId))
            .horizonDays(horizonDays)
            .asOfDate(asOfDate.toString())
            .returnDaily(true)
            .build();

        ObjectMapper mapper = new ObjectMapper();

        try {System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));} 
        catch (Exception exception) {}

        return webClient.post()
            .uri("http://localhost:8100/forecast")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

}
