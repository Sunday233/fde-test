package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("复核操作表")
public class VerificationOperation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("house_id")
    private String houseId;

    @TableField("order_id")
    private String orderId;

    @TableField("operate_type_name")
    private String operateTypeName;

    @TableField("order_status_desc")
    private String orderStatusDesc;

    @TableField("operation_time")
    private LocalDateTime operationTime;
}
