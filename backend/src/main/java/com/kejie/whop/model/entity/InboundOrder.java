package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("入库单表")
public class InboundOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("入库单号")
    private String inboundOrderNo;

    @TableField("标记")
    private String mark;

    @TableField("订单号")
    private String orderNo;

    @TableField("外部单据号")
    private String externalDocNo;

    @TableField("交易单号")
    private String transactionNo;

    @TableField("单据状态")
    private String docStatus;

    @TableField("状态")
    private String status;

    @TableField("待上架确认")
    private String pendingShelvingConfirm;

    @TableField("过账状态")
    private String postingStatus;

    @TableField("是否允许部分过账")
    private String allowPartialPosting;

    @TableField("单据类型")
    private String docType;

    @TableField("类型名称")
    private String typeName;

    @TableField("退货运单号")
    private String returnTrackingNo;

    @TableField("已收数量")
    private BigDecimal receivedQty;

    @TableField("未收数量")
    private BigDecimal unreceivedQty;

    @TableField("物料总数量")
    private BigDecimal totalMaterialQty;

    @TableField("应收数量")
    private BigDecimal expectedQty;

    @TableField("已清点数量")
    private BigDecimal countedQty;

    @TableField("工厂编码")
    private String factoryCode;

    @TableField("工厂")
    private String factory;

    @TableField("店铺编码")
    private String shopCode;

    @TableField("店铺名称")
    private String shopName;

    @TableField("库房编码")
    private String warehouseCode;

    @TableField("库房")
    private String warehouse;

    @TableField("创单时间")
    private LocalDateTime createTime;

    @TableField("过账时间")
    private LocalDateTime postingTime;

    @TableField("修改时间")
    private LocalDateTime modifyTime;

    @TableField("计划入库时间")
    private LocalDateTime plannedInboundTime;

    @TableField("总金额")
    private BigDecimal totalAmount;

    @TableField("备注")
    private String remark;

    @TableField("自定义一")
    private String custom1;

    @TableField("自定义二")
    private String custom2;

    @TableField("自定义三")
    private String custom3;

    @TableField("自定义四")
    private String custom4;

    @TableField("是否运输")
    private String isTransport;
}
