package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OperationFeeDetailVO {
    private String dimension;
    private String date;
    private Integer year;
    private Integer month;
    private String monthLabel;
    private String warehouseCode;
    private String warehouseName;
    private String operationCategory;
    private Long operationCount;
    private BigDecimal feeRatio;
    private BigDecimal estimatedFee;
}