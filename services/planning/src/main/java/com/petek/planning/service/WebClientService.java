package com.petek.planning.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.petek.planning.dto.ForecastRequest;


@Service
public class WebClientService {
    
    private final RestTemplate restTemplate;
    
    public WebClientService() {
        this.restTemplate = new RestTemplate();
    }

    public String getProduct(Long productId) {
        return restTemplate.getForObject(
            "http://localhost:8000/api/v1/products/" + productId, 
            String.class
        );
    }

    public String getSuppliers(Long productId) {
        return restTemplate.getForObject(
            "http://localhost:8000/api/v1/products/" + productId + "/suppliers", 
            String.class
        );
    }

    public String getForecasts(Integer productId, int horizonDays, LocalDate asOfDate) {
        ForecastRequest request = ForecastRequest.builder()
            .productIds(List.of(productId))
            .horizonDays(horizonDays)
            .asOfDate(asOfDate.toString())
            .returnDaily(true)
            .build();

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        // Create HTTP entity with request body and headers
        HttpEntity<ForecastRequest> entity = new HttpEntity<>(request, headers);

        // Make the POST request
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8100/forecast",
            entity,
            String.class
        );

        return response.getBody();
    }

}
