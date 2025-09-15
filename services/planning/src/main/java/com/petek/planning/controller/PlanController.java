package com.petek.planning.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.planning.dto.PlanRequest;
import com.petek.planning.dto.PlanResponse;
import com.petek.planning.service.PlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {
    
    private final PlanService service;

    /**
     * Get ollama output
     */
    @GetMapping
    public ResponseEntity<PlanResponse> getOllamaOutput(
        @ModelAttribute @Valid PlanRequest request
    ) {
        return ResponseEntity.ok(service.getAiOutput(request));
    }

}
