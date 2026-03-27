package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("上架单表")
public class ShelvingOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("上架单号")
    private String shelvingOrderNo;

    @TableField("入库单号")
    private String inboundOrderNo;

    @TableField("上架单类型")
    private String shelvingOrderType;

    @TableField("上架单状态")
    private String shelvingOrderStatus;

    @TableField("库房编码")
    private String warehouseCode;

    @TableField("库房")
    private String warehouse;

    @TableField("创建人员")
    private String createdBy;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("确认时间")
    private LocalDateTime confirmTime;

    @TableField("过账状态")
    private String postingStatus;

    @TableField("确认人")
    private String confirmedBy;

    @TableField("上架单总数量")
    private BigDecimal totalShelvingQty;
}
