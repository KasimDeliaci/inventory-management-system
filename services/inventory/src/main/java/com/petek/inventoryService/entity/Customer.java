package com.petek.inventoryService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

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
@SQLDelete(sql = "UPDATE customers SET deleted_at = NOW() WHERE customer_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;
    
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_segment", nullable = false, columnDefinition = "customer_segment")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CustomerSegment customerSegment;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 30)
    private String phone;
    
    @Column(length = 50)
    private String city;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum CustomerSegment {
        INDIVIDUAL, SME, CORPORATE, ENTERPRISE, OTHER
    }
}
