package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("出库单表")
public class OutboundOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("出库单号")
    private String outboundOrderNo;

    @TableField("单据类型")
    private String docType;

    @TableField("标记类型")
    private String markType;

    @TableField("标记备注")
    private String markRemark;

    @TableField("工厂编码")
    private String factoryCode;

    @TableField("工厂")
    private String factory;

    @TableField("外部单据号")
    private String externalDocNo;

    @TableField("交易单号")
    private String transactionNo;

    @TableField("店铺编码")
    private String shopCode;

    @TableField("店铺名称")
    private String shopName;

    @TableField("店铺组")
    private String shopGroup;

    @TableField("平台")
    private String platform;

    @TableField("预售状态")
    private String presaleStatus;

    @TableField("单据状态")
    private String docStatus;

    @TableField("加工状态")
    private String processingStatus;

    @TableField("是否交接")
    private String isHandover;

    @TableField("是否称重")
    private String isWeighed;

    @TableField("称重时间")
    private LocalDateTime weighTime;

    @TableField("发货状态")
    private String shipmentStatus;

    @TableField("发货时间")
    private LocalDateTime shipmentTime;

    @TableField("状态")
    private String status;

    @TableField("锁定节点")
    private String lockNode;

    @TableField("挂起原因")
    private String suspendReason;

    @TableField("挂起方式")
    private String suspendMethod;

    @TableField("付款时间")
    private LocalDateTime paymentTime;

    @TableField("创建时间")
    private LocalDateTime createTime;

    @TableField("修改时间")
    private LocalDateTime modifyTime;

    @TableField("发货考核时间")
    private LocalDateTime shipmentAssessTime;

    @TableField("揽收考核时间")
    private LocalDateTime pickupAssessTime;

    @TableField("入首分拨考核时间")
    private LocalDateTime firstSortAssessTime;

    @TableField("复核完成时间")
    private LocalDateTime verifyCompleteTime;

    @TableField("交接时间")
    private LocalDateTime handoverTime;

    @TableField("撤单时间")
    private LocalDateTime cancelTime;

    @TableField("合单标识")
    private String mergeFlag;

    @TableField("合单号")
    private String mergeNo;

    @TableField("后置合箱标记")
    private String postMergeMark;

    @TableField("客户指定物流")
    private String customerLogistics;

    @TableField("客户指定承运商")
    private String customerCarrier;

    @TableField("指定运单号")
    private String assignedTrackingNo;

    @TableField("指定辅材类型")
    private String assignedAuxMaterialType;

    @TableField("调度承运商")
    private String dispatchCarrier;

    @TableField("调度运单号")
    private String dispatchTrackingNo;

    @TableField("拣货单号")
    private String pickingOrderNo;

    @TableField("格子号")
    private String gridNo;

    @TableField("推荐包材编码")
    private String recommendPackCode;

    @TableField("推荐包材名称")
    private String recommendPackName;

    @TableField("推荐包材PN")
    private String recommendPackPn;

    @TableField("包材推荐状态")
    private String packRecommendStatus;

    @TableField("包材推荐异常信息")
    private String packRecommendError;

    @TableField("使用包材编码")
    private String usedPackCode;

    @TableField("使用包材名称")
    private String usedPackName;

    @TableField("使用包材PN")
    private String usedPackPn;

    @TableField("总箱数")
    private String totalBoxCount;

    @TableField("已复核数量")
    private String verifiedQty;

    @TableField("未复核数量")
    private String unverifiedQty;

    @TableField("物料总数量")
    private String totalMaterialQty;

    @TableField("总金额")
    private String totalAmount;

    @TableField("理论重量(KG)")
    private String theoreticalWeight;

    @TableField("关联拆单总重量")
    private String relatedSplitTotalWeight;

    @TableField("理论体积(立方分米)")
    private String theoreticalVolume;

    @TableField("关联拆单总体积")
    private String relatedSplitTotalVolume;

    @TableField("实际重量(KG)")
    private String actualWeight;

    @TableField("实际体积(立方分米)")
    private String actualVolume;

    @TableField("最大包装理论重量(KG)")
    private String maxPackTheoreticalWeight;

    @TableField("最大包装理论体积(立方分米)")
    private String maxPackTheoreticalVolume;

    @TableField("卖家备注")
    private String sellerRemark;

    @TableField("行业属性")
    private String industryAttribute;

    @TableField("订单号")
    private String orderNo;

    @TableField("保价标识")
    private String insuredFlag;

    @TableField("保价金额")
    private String insuredAmount;

    @TableField("备注")
    private String remark;

    @TableField("自定义1")
    private String custom1;

    @TableField("自定义2")
    private String custom2;

    @TableField("自定义3")
    private String custom3;

    @TableField("自定义4")
    private String custom4;

    @TableField("自定义5")
    private String custom5;

    @TableField("附件")
    private String attachment;

    @TableField("COD标识")
    private String codFlag;

    @TableField("拆箱标识")
    private String unpackFlag;

    @TableField("买方")
    private String buyer;

    @TableField("签收要求")
    private String signRequirement;

    @TableField("运输要求")
    private String transportRequirement;

    @TableField("加工要求")
    private String processingRequirement;

    @TableField("淘天物流标记")
    private String taotianLogisticsMark;

    @TableField("淘天指定承运商")
    private String taotianCarrier;

    @TableField("库房编码")
    private String warehouseCode;

    @TableField("库房名称")
    private String warehouseName;

    @TableField("收货方编码")
    private String receiverCode;

    @TableField("收货方名称")
    private String receiverName;

    @TableField("收货方联系人")
    private String receiverContact;

    @TableField("收货方电话")
    private String receiverPhone;

    @TableField("收货方手机")
    private String receiverMobile;

    @TableField("收货方省份")
    private String receiverProvince;

    @TableField("收货方城市")
    private String receiverCity;

    @TableField("收货方区域")
    private String receiverDistrict;

    @TableField("收货方街道")
    private String receiverStreet;

    @TableField("收货方地址")
    private String receiverAddress;

    @TableField("前置拆单标识")
    private String preSplitFlag;

    @TableField("手动回传标识")
    private String manualUploadFlag;

    @TableField("物料编码形态")
    private String materialCodeForm;

    @TableField("外部物料号形态")
    private String externalMaterialNoForm;

    @TableField("物料名称形态")
    private String materialNameForm;
}
