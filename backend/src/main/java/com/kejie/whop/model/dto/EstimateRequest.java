package com.kejie.whop.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EstimateRequest {

    @NotNull
    @Positive
    private BigDecimal dailyOrders;

    @NotNull
    @Positive
    private BigDecimal itemsPerOrder;

    @NotNull
    @Positive
    private Integer workDays;

    @NotNull
    @Positive
    private BigDecimal laborEfficiency;

    @NotNull
    @Positive
    private BigDecimal fixedLaborPrice;

    @NotNull
    @Positive
    private BigDecimal tempLaborPrice;

    private BigDecimal forkliftLaborPrice = BigDecimal.ZERO;

    @NotNull
    private BigDecimal fixedLaborRatio;

    private BigDecimal tempLaborRatio;

    private BigDecimal forkliftLaborRatio = BigDecimal.ZERO;

    private BigDecimal hoursPerDay = new BigDecimal("8");

    private BigDecimal itemsPerOrderBaseline;

    private Boolean enableItemCorrection = false;

    private BigDecimal taxRate = new BigDecimal("0.06");
}
