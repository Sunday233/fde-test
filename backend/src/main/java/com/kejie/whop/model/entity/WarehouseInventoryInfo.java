package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("仓位库存信息表")
public class WarehouseInventoryInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("库房")
    private String warehouse;

    @TableField("工厂名称")
    private String factoryName;

    @TableField("外部物料号")
    private String externalMaterialNo;

    @TableField("物料名称")
    private String materialName;

    @TableField("区域")
    private String area;

    @TableField("仓位号")
    private String positionNo;

    @TableField("库存地编码")
    private String storageLocationCode;

    @TableField("库存地")
    private String storageLocation;

    @TableField("可用库存")
    private BigDecimal availableStock;

    @TableField("生产日期")
    private LocalDate productionDate;

    @TableField("到期日期")
    private LocalDate expiryDate;

    @TableField("批号")
    private String batchNo;

    @TableField("剩余有效期天数")
    private Integer remainingShelfLifeDays;
}
