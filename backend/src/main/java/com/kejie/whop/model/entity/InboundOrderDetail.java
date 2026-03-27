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
@TableName("入库单行明细表")
public class InboundOrderDetail {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("入库单号")
    private String inboundOrderNo;

    @TableField("外部单据号")
    private String externalDocNo;

    @TableField("订单号")
    private String orderNo;

    @TableField("状态")
    private String status;

    @TableField("过账状态")
    private String postingStatus;

    @TableField("单据类型名称")
    private String docTypeName;

    @TableField("退货运单号")
    private String returnTrackingNo;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("过账时间")
    private LocalDateTime postingTime;

    @TableField("入库单备注")
    private String inboundRemark;

    @TableField("行号")
    private String lineNo;

    @TableField("工厂编码")
    private String factoryCode;

    @TableField("工厂")
    private String factory;

    @TableField("库房编码")
    private String warehouseCode;

    @TableField("库房")
    private String warehouse;

    @TableField("物料编码")
    private String materialCode;

    @TableField("外部物料号")
    private String externalMaterialNo;

    @TableField("物料PN")
    private String materialPn;

    @TableField("物料名称")
    private String materialName;

    @TableField("物料简称")
    private String materialShortName;

    @TableField("总数量")
    private BigDecimal totalQty;

    @TableField("已收数量")
    private BigDecimal receivedQty;

    @TableField("未收数量")
    private BigDecimal unreceivedQty;

    @TableField("库存地编码")
    private String storageLocationCode;

    @TableField("库存地")
    private String storageLocation;

    @TableField("单位")
    private String unit;

    @TableField("最小单位数量")
    private BigDecimal minUnitQty;

    @TableField("最大单位数量")
    private String maxUnitQty;

    @TableField("过账数量")
    private BigDecimal postingQty;

    @TableField("物料状态")
    private String materialStatus;

    @TableField("正残属性")
    private String qualityProperty;

    @TableField("重量")
    private BigDecimal weight;

    @TableField("体积")
    private BigDecimal volume;

    @TableField("单价")
    private BigDecimal unitPrice;

    @TableField("金额")
    private BigDecimal amount;

    @TableField("批号")
    private String batchNo;

    @TableField("生产日期")
    private LocalDate productionDate;

    @TableField("到期日期")
    private LocalDate expiryDate;

    @TableField("批次1")
    private String batch1;

    @TableField("批次2")
    private String batch2;

    @TableField("批次3")
    private String batch3;

    @TableField("批次4")
    private String batch4;

    @TableField("批次5")
    private String batch5;

    @TableField("批次6")
    private String batch6;

    @TableField("批次7")
    private String batch7;

    @TableField("批次8")
    private String batch8;

    @TableField("批次9")
    private String batch9;

    @TableField("批次10")
    private String batch10;

    @TableField("行备注一")
    private String lineRemark1;

    @TableField("行备注二")
    private String lineRemark2;

    @TableField("行备注三")
    private String lineRemark3;

    @TableField("是否退款")
    private String isRefund;
}
