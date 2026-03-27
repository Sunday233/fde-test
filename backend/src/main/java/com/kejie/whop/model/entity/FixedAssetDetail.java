package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("在账资产明细表")
public class FixedAssetDetail {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("挂账人")
    private String accountHolder;

    @TableField("ITCODE")
    private String itcode;

    @TableField("预算单元")
    private String budgetUnit;

    @TableField("平台")
    private String platform;

    @TableField("使用地名称")
    private String usageLocationName;

    @TableField("资产类别")
    private String assetCategory;

    @TableField("资产一级分类")
    private String assetClass1;

    @TableField("资产二级分类")
    private String assetClass2;

    @TableField("资产编码")
    private String assetCode;

    @TableField("资产名称")
    private String assetName;

    @TableField("资产数量")
    private String assetQty;

    @TableField("采购单价")
    private String purchaseUnitPrice;

    @TableField("资产残余总价")
    private String assetResidualValue;

    @TableField("摊销时间（月）")
    private String amortizationMonths;

    @TableField("残值系数")
    private String residualValueCoeff;

    @TableField("成本中心编码")
    private String costCenterCode;

    @TableField("成本中心名称")
    private String costCenterName;

    @TableField("入库日期")
    private LocalDate inboundDate;

    @TableField("入账日期")
    private LocalDate accountingDate;

    @TableField("入库单号")
    private String inboundOrderNo;

    @TableField("公司代码")
    private String companyCode;

    @TableField("资产号")
    private String assetNo;

    @TableField("子号")
    private String subNo;

    @TableField("单位")
    private String unit;

    @TableField("规格")
    private String specification;

    @TableField("型号")
    private String model;

    @TableField("供应商")
    private String supplier;

    @TableField("库存状态")
    private String inventoryStatus;

    @TableField("入账状态")
    private String accountingStatus;

    @TableField("唯一码管理")
    private String uniqueCodeManagement;

    @TableField("资产当前状态")
    private String assetCurrentStatus;
}
