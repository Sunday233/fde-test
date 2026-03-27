package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardOverviewVO {

    private String warehouseCode;
    private String warehouseName;
    private String month;
    private Long totalOrders;
    private Long totalItems;
    private BigDecimal totalWorkHours;
    private BigDecimal monthlyFee;
    private BigDecimal laborEfficiency;
    private BigDecimal avgCostPerOrder;
    private BigDecimal avgCostPerItem;
    private Integer avgHeadcount;
    private BigDecimal dailyAvgFee;
    private Integer workDays;
    private BigDecimal itemsPerOrder;
}
