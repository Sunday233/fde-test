package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("退货信息表")
public class ReturnInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("入库单号")
    private String inboundOrderNo;

    @TableField("退货运单号")
    private String returnTrackingNo;

    @TableField("原出库单号")
    private String originalOutboundNo;

    @TableField("原外部单据号")
    private String originalExternalDocNo;

    @TableField("原交易单号")
    private String originalTransactionNo;

    @TableField("寄件人姓名")
    private String senderName;

    @TableField("寄件人电话")
    private String senderPhone;

    @TableField("退货原因")
    private String returnReason;
}
