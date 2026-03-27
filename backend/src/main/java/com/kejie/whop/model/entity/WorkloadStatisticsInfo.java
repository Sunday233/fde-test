package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("工作量统计信息表")
public class WorkloadStatisticsInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("单量")
    private String orderCount;

    @TableField("包含单据数量")
    private String containedDocCount;

    @TableField("物料数量")
    private String materialCount;

    @TableField("仓位数")
    private String positionCount;

    @TableField("重量")
    private String weight;

    @TableField("工厂编码")
    private String factoryCode;

    @TableField("工厂")
    private String factory;

    @TableField("店铺编码")
    private String shopCode;

    @TableField("店铺名称")
    private String shopName;

    @TableField("单据类型")
    private String docType;

    @TableField("拣货单类型")
    private String pickingOrderType;

    @TableField("登录账号")
    private String loginAccount;

    @TableField("员工标识")
    private String employeeId;

    @TableField("员工姓名")
    private String employeeName;

    @TableField("操作类型")
    private String operationType;

    @TableField("操作大类")
    private String operationCategory;
}
