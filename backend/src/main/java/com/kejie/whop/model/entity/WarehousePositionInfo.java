package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("仓位信息表")
public class WarehousePositionInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("出库单号")
    private String outboundOrderNo;

    @TableField("行项目")
    private String lineItem;

    @TableField("物料编码")
    private String materialCode;

    @TableField("物料名称")
    private String materialName;

    @TableField("外部物料号")
    private String externalMaterialNo;

    @TableField("PN")
    private String pn;

    @TableField("仓位")
    private String position;

    @TableField("容器")
    private String container;

    @TableField("数量")
    private String qty;

    @TableField("工厂编码")
    private String factoryCode;

    @TableField("工厂")
    private String factory;

    @TableField("库存地编码")
    private String storageLocationCode;

    @TableField("库存地")
    private String storageLocation;

    @TableField("物料状态")
    private String materialStatus;

    @TableField("正残属性")
    private String qualityProperty;

    @TableField("批属性")
    private String batchAttribute;

    @TableField("入库时间")
    private LocalDateTime inboundTime;

    @TableField("出库标识")
    private String outboundFlag;

    @TableField("生产日期")
    private LocalDate productionDate;

    @TableField("到期日期")
    private LocalDate expiryDate;

    @TableField("批次")
    private String batchNo;

    @TableField("批属性1")
    private String batchAttr1;

    @TableField("批属性2")
    private String batchAttr2;

    @TableField("批属性3")
    private String batchAttr3;

    @TableField("批属性4")
    private String batchAttr4;

    @TableField("批属性5")
    private String batchAttr5;

    @TableField("批属性6")
    private String batchAttr6;

    @TableField("批属性7")
    private String batchAttr7;

    @TableField("批属性8")
    private String batchAttr8;

    @TableField("批属性9")
    private String batchAttr9;

    @TableField("批属性10")
    private String batchAttr10;
}
