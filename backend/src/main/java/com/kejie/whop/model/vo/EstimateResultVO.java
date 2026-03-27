package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EstimateResultVO {

    private Integer estimatedHeadcount;
    private BigDecimal estimatedTotalHours;
    private BigDecimal weightedUnitPrice;
    private BigDecimal monthlyFee;
    private BigDecimal dailyFee;
    private BigDecimal costPerOrder;
    private BigDecimal costPerItem;
    private BigDecimal itemCorrectionFactor;
    private BigDecimal baselineDailyFee;
    private BigDecimal baselineDeviation;
}
