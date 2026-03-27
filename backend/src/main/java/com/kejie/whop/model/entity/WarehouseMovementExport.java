package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("库内移动导出表")
public class WarehouseMovementExport {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("库房")
    private String warehouse;

    @TableField("任务号")
    private String taskNo;

    @TableField("移动类型")
    private String movementType;

    @TableField("状态")
    private String status;

    @TableField("单据状态")
    private String docStatus;

    @TableField("创单时间")
    private LocalDateTime createTime;

    @TableField("工厂编码")
    private String factoryCode;

    @TableField("工厂")
    private String factory;

    @TableField("行号")
    private String lineNo;

    @TableField("物料编码")
    private String materialCode;

    @TableField("物料名称")
    private String materialName;

    @TableField("外部物料号")
    private String externalMaterialNo;

    @TableField("移出仓位")
    private String fromPosition;

    @TableField("移出容器")
    private String fromContainer;

    @TableField("目的仓位")
    private String toPosition;

    @TableField("目的容器")
    private String toContainer;

    @TableField("数量")
    private String qty;

    @TableField("单位")
    private String unit;

    @TableField("最小单位数量")
    private String minUnitQty;

    @TableField("库存地")
    private String storageLocation;

    @TableField("正残属性")
    private String qualityProperty;

    @TableField("物料状态")
    private String materialStatus;

    @TableField("批属性")
    private String batchAttribute;

    @TableField("生产日期")
    private LocalDate productionDate;

    @TableField("到期日期")
    private LocalDate expiryDate;

    @TableField("批号")
    private String batchNo;

    @TableField("自定义批属性1")
    private String customBatchAttr1;

    @TableField("自定义批属性2")
    private String customBatchAttr2;

    @TableField("自定义批属性3")
    private String customBatchAttr3;

    @TableField("自定义批属性4")
    private String customBatchAttr4;

    @TableField("自定义批属性5")
    private String customBatchAttr5;

    @TableField("自定义批属性6")
    private String customBatchAttr6;

    @TableField("自定义批属性7")
    private String customBatchAttr7;

    @TableField("自定义批属性8")
    private String customBatchAttr8;

    @TableField("自定义批属性9")
    private String customBatchAttr9;

    @TableField("自定义批属性10")
    private String customBatchAttr10;

    @TableField("自定义批属性11")
    private String customBatchAttr11;

    @TableField("自定义批属性12")
    private String customBatchAttr12;

    @TableField("自定义批属性13")
    private String customBatchAttr13;

    @TableField("自定义批属性14")
    private String customBatchAttr14;

    @TableField("自定义批属性15")
    private String customBatchAttr15;

    @TableField("自定义批属性16")
    private String customBatchAttr16;

    @TableField("自定义批属性17")
    private String customBatchAttr17;

    @TableField("自定义批属性18")
    private String customBatchAttr18;

    @TableField("自定义批属性19")
    private String customBatchAttr19;

    @TableField("自定义批属性20")
    private String customBatchAttr20;
}
