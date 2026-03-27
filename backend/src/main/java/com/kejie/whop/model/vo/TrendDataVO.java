package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrendDataVO {

    private String date;
    private String warehouseCode;
    private String warehouseName;
    private BigDecimal value;
    private String type;
}
