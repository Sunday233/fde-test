package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyBaselineVO {

    private String warehouseCode;
    private String warehouseName;
    private Integer year;
    private Integer month;
    private BigDecimal dailyAvgFee;
    private BigDecimal totalFee;
    private Long totalOrders;
    private Long totalItems;
    private BigDecimal costPerOrder;
    private BigDecimal costPerItem;
    private Integer avgHeadcount;
    private BigDecimal totalWorkHours;
    private BigDecimal weightedUnitPrice;
    private BigDecimal itemsPerOrder;
    private BigDecimal dailyAvgOrders;
    private BigDecimal fixedTempRatio;
    private BigDecimal laborEfficiency;
    private String warehouseType;
}
