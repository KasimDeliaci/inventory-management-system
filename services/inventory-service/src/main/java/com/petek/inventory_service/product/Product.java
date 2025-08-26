package com.petek.inventory_service.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class Product {
    @Id
    @GeneratedValue
    private String product_id;
    private String product_name;
    private String description;
    private String category;
    private String unit_of_measure;
    private double safety_stock;
    private double reorder_point;
    private double current_price;
}
