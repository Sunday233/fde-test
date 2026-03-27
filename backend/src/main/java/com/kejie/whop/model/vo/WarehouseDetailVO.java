package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class WarehouseDetailVO {

    private String warehouseCode;
    private String warehouseName;
    private Integer year;
    private Integer month;
    private BigDecimal totalFee;
    private BigDecimal dailyAvgFee;
    private Long totalOrders;
    private Long totalItems;
    private BigDecimal costPerOrder;
    private BigDecimal costPerItem;
    private BigDecimal itemsPerOrder;
    private BigDecimal dailyAvgOrders;
    private Integer avgHeadcount;
    private BigDecimal totalWorkHours;
    private BigDecimal weightedUnitPrice;
    private Map<String, BigDecimal> workHoursBreakdown;
    private Map<String, Object> laborDistribution;
}
