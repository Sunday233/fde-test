package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("报价信息表")
public class QuotationInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("报价编码")
    private String quotationCode;

    @TableField("合同编码")
    private String contractCode;

    @TableField("平台名称")
    private String platformName;

    @TableField("库房名称")
    private String warehouseName;

    @TableField("供应商名称")
    private String supplierName;

    @TableField("计价方式")
    private String pricingMethod;

    @TableField("税率")
    private BigDecimal taxRate;

    @TableField("报价状态")
    private String quotationStatus;

    @TableField("结费类型")
    private String settlementType;

    @TableField("班次类别")
    private String shiftCategory;

    @TableField("供应商结算单价")
    private BigDecimal supplierSettlementPrice;

    @TableField("报价开始日期")
    private LocalDate quotationStartDate;

    @TableField("报价结束日期")
    private LocalDate quotationEndDate;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("更新时间")
    private LocalDateTime updateTime;
}
