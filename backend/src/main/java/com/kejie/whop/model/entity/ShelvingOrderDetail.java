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
@TableName("上架单明细表")
public class ShelvingOrderDetail {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("上架单号")
    private String shelvingOrderNo;

    @TableField("上架单行号")
    private String shelvingLineNo;

    @TableField("入库单号")
    private String inboundOrderNo;

    @TableField("入库单行号")
    private String inboundLineNo;

    @TableField("上架状态")
    private String shelvingStatus;

    @TableField("数量")
    private BigDecimal qty;

    @TableField("已上架数量")
    private BigDecimal shelvedQty;

    @TableField("物料编码")
    private String materialCode;

    @TableField("物料PN")
    private String materialPn;

    @TableField("目的仓位")
    private String targetPosition;

    @TableField("目的容器")
    private String targetContainer;

    @TableField("库存地编码")
    private String storageLocationCode;

    @TableField("库存地")
    private String storageLocation;

    @TableField("正残属性")
    private String qualityProperty;

    @TableField("物料状态")
    private String materialStatus;

    @TableField("物料名称")
    private String materialName;

    @TableField("周转容器")
    private String turnoverContainer;

    @TableField("生产日期")
    private LocalDate productionDate;

    @TableField("到期日期")
    private LocalDate expiryDate;

    @TableField("收货工厂编码")
    private String receivingFactoryCode;

    @TableField("收货工厂")
    private String receivingFactory;

    @TableField("过账状态")
    private String postingStatus;

    @TableField("批号")
    private String batchNo;

    @TableField("批属性一")
    private String batchAttr1;

    @TableField("批属性二")
    private String batchAttr2;

    @TableField("批属性三")
    private String batchAttr3;

    @TableField("批属性四")
    private String batchAttr4;

    @TableField("批属性五")
    private String batchAttr5;

    @TableField("批属性六")
    private String batchAttr6;

    @TableField("批属性七")
    private String batchAttr7;

    @TableField("批属性八")
    private String batchAttr8;

    @TableField("批属性九")
    private String batchAttr9;

    @TableField("批属性十")
    private String batchAttr10;

    @TableField("批属性十一")
    private String batchAttr11;

    @TableField("批属性十二")
    private String batchAttr12;

    @TableField("批属性十三")
    private String batchAttr13;

    @TableField("批属性十四")
    private String batchAttr14;

    @TableField("批属性十五")
    private String batchAttr15;

    @TableField("批属性十六")
    private String batchAttr16;

    @TableField("批属性十七")
    private String batchAttr17;

    @TableField("批属性十八")
    private String batchAttr18;

    @TableField("批属性十九")
    private String batchAttr19;

    @TableField("批属性二十")
    private String batchAttr20;

    @TableField("外部物料号")
    private String externalMaterialNo;

    @TableField("外部单据号")
    private String externalDocNo;

    @TableField("确认上架日期")
    private LocalDateTime confirmShelvingDate;
}
