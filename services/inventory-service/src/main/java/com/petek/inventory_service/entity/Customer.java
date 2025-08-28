package com.petek.inventory_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "customers")
public class Customer {
    
    public enum CustomerSegment {
        INDIVIDUAL, SME, CORPORATE, ENTERPRISE, OTHER
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;
    
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_segment")
    private CustomerSegment customerSegment;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 30)
    private String phone;
    
    @Column(length = 50)
    private String city;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
