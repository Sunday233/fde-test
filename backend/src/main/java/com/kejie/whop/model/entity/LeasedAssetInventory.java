package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("租赁资产库存导出表")
public class LeasedAssetInventory {

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

    @TableField("首次租赁单价")
    private String firstLeaseUnitPrice;

    @TableField("租赁总价")
    private String totalLeasePrice;

    @TableField("租期类型")
    private String leaseTermType;

    @TableField("成本中心编码")
    private String costCenterCode;

    @TableField("成本中心名称")
    private String costCenterName;

    @TableField("入库日期")
    private LocalDate inboundDate;

    @TableField("入库单号")
    private String inboundOrderNo;

    @TableField("单位")
    private String unit;

    @TableField("规格")
    private String specification;

    @TableField("型号")
    private String model;

    @TableField("供应商")
    private String supplier;

    @TableField("实际使用人")
    private String actualUser;

    @TableField("租赁开始日期")
    private LocalDate leaseStartDate;

    @TableField("租赁结束日期")
    private LocalDate leaseEndDate;

    @TableField("是否续租")
    private String isRenewed;

    @TableField("库存行ID")
    private String inventoryLineId;
}
