package com.kejie.whop.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DailyDetailVO {
    private String date;
    private String warehouseCode;
    private String warehouseName;
    private Long obOrders;
    private Long obItems;
    private BigDecimal itemOrderRatio;
    private Integer headcount;
    private BigDecimal workHours;
    private BigDecimal dailyFee;
}
