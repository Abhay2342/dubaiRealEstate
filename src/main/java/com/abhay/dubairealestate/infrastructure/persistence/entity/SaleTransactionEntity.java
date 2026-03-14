package com.abhay.dubairealestate.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trans_id")
    private String transId;

    @Column(name = "trans_date")
    private LocalDate transDate;

    @Column(name = "trans_value", precision = 20, scale = 2)
    private BigDecimal transValue;

    @Column(name = "area_name", length = 200)
    private String areaName;

    @Column(name = "project_name", length = 200)
    private String projectName;

    @Column(name = "usage", length = 100)
    private String usage;

    @Column(name = "registration_type", length = 100)
    private String registrationType;

    @Column(name = "property_type", length = 100)
    private String propertyType;

    @Column(name = "rooms", length = 50)
    private String rooms;

    @Column(name = "actual_area", precision = 15, scale = 2)
    private BigDecimal actualArea;

    @Column(name = "meter_sale_price", precision = 20, scale = 2)
    private BigDecimal meterSalePrice;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
