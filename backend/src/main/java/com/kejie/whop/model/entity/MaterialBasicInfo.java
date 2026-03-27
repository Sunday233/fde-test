package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("物料基本信息表")
public class MaterialBasicInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("工厂编码")
    private String factoryCode;

    @TableField("工厂")
    private String factory;

    @TableField("外部物料号")
    private String externalMaterialNo;

    @TableField("商城物料号")
    private String mallMaterialNo;

    @TableField("物料编码")
    private String materialCode;

    @TableField("物料名称")
    private String materialName;

    @TableField("物料简称")
    private String materialShortName;

    @TableField("计量单位")
    private String measureUnit;

    @TableField("品牌名称")
    private String brandName;

    @TableField("物料类型名称")
    private String materialTypeName;

    @TableField("款号")
    private String styleNo;

    @TableField("规格")
    private String specification;

    @TableField("物料PN")
    private String materialPn;

    @TableField("是否仓储维护")
    private String isWarehouseMaintained;

    @TableField("长度(cm)")
    private String lengthCm;

    @TableField("宽度(cm)")
    private String widthCm;

    @TableField("高度(cm)")
    private String heightCm;

    @TableField("体积(CDM)")
    private String volumeCdm;

    @TableField("毛重(KG)")
    private String grossWeightKg;

    @TableField("净重(KG)")
    private String netWeightKg;

    @TableField("标准托盘数量")
    private String standardPalletQty;

    @TableField("包材冗余系数")
    private String packRedundancyCoeff;

    @TableField("重量误差计算模式")
    private String weightErrorCalcMode;

    @TableField("允许称重误差")
    private String allowedWeightError;

    @TableField("ABC分类")
    private String abcClassification;

    @TableField("在库周期管理")
    private String storageCycleManagement;

    @TableField("允许物料混放")
    private String allowMaterialMix;

    @TableField("允许批属性混放")
    private String allowBatchMix;

    @TableField("允许库存地混放")
    private String allowStorageMix;

    @TableField("允许正残混放")
    private String allowQualityMix;

    @TableField("允许物料状态混放")
    private String allowStatusMix;

    @TableField("允许入库日期混放")
    private String allowInboundDateMix;

    @TableField("允许库存地类型混放")
    private String allowStorageTypeMix;

    @TableField("标签管理")
    private String labelManagement;

    @TableField("套餐物料")
    private String bundleMaterial;

    @TableField("批属性代码")
    private String batchAttrCode;

    @TableField("批属性名称")
    private String batchAttrName;

    @TableField("保质期管理")
    private String shelfLifeManagement;

    @TableField("保质期天数")
    private String shelfLifeDays;

    @TableField("保质期预警系数")
    private String shelfLifeWarningCoeff;

    @TableField("收货有效期控制模式")
    private String receivingShelfLifeMode;

    @TableField("收货有效期控制天数")
    private String receivingShelfLifeDays;

    @TableField("发货有效期控制模式")
    private String shippingShelfLifeMode;

    @TableField("发货有效期控制天数")
    private String shippingShelfLifeDays;

    @TableField("批次号管理")
    private String batchNoManagement;

    @TableField("自定义批次1")
    private String customBatch1;

    @TableField("自定义批次2")
    private String customBatch2;

    @TableField("自定义批次3")
    private String customBatch3;

    @TableField("自定义批次4")
    private String customBatch4;

    @TableField("自定义批次5")
    private String customBatch5;

    @TableField("包装方案")
    private String packagingPlan;

    @TableField("原箱标记")
    private String originalBoxMark;

    @TableField("包材品类")
    private String packMaterialCategory;

    @TableField("物料分类")
    private String materialClassification;

    @TableField("创建人")
    private String createdBy;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("修改人")
    private String modifiedBy;

    @TableField("修改时间")
    private LocalDateTime modifyTime;
}
