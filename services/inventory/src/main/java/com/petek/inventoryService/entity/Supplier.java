package com.petek.inventoryService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
@Table(name = "suppliers")
@SQLDelete(sql = "UPDATE suppliers SET deleted_at = NOW() WHERE supplier_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;
    
    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;
    
    @Column(length = 100, nullable = false)
    private String email;
    
    @Column(length = 30, nullable = false)
    private String phone;
    
    @Column(length = 50, nullable = false)
    private String city;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<ProductSupplier> productSuppliers;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
