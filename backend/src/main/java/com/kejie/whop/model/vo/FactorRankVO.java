package com.kejie.whop.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FactorRankVO {

    private Integer rank;
    private String factorName;
    private BigDecimal correlation;
    private String description;
    private Integer tier;
    private String tierLabel;
    private String applicationTip;
}
