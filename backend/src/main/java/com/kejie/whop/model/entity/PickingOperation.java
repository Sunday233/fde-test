package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("拣货操作表")
public class PickingOperation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("house_id")
    private String houseId;

    @TableField("order_id")
    private String orderId;

    @TableField("operate_type_name")
    private String operateTypeName;

    @TableField("operation_time")
    private LocalDateTime operationTime;
}
