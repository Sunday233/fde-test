package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompareResultVO {

    private String date;
    private Integer year;
    private Integer month;
    private String warehouseCode;
    private String warehouseName;
    private BigDecimal totalFee;
    private Long totalOrders;
    private Integer avgHeadcount;
    private BigDecimal dailyAvgFee;
    private BigDecimal costPerOrder;
    private BigDecimal costPerItem;
    private BigDecimal laborEfficiency;
    private BigDecimal totalWorkHours;
}
