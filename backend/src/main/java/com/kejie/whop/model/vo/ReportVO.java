package com.kejie.whop.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportVO {

    private String id;
    private String title;
    private String warehouseCode;
    private String warehouseName;
    private String startMonth;
    private String endMonth;
    private String content;
    private LocalDateTime createdAt;
}
