package com.kejie.whop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("出勤统计表")
public class AttendanceStatistics {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("平台")
    private String platform;

    @TableField("库房")
    private String warehouse;

    @TableField("部门")
    private String department;

    @TableField("项目")
    private String project;

    @TableField("岗位")
    private String position;

    @TableField("员工编码")
    private String employeeCode;

    @TableField("员工身份证")
    private String employeeIdCard;

    @TableField("员工姓名")
    private String employeeName;

    @TableField("员工类型")
    private String employeeType;

    @TableField("计价方式")
    private String pricingMethod;

    @TableField("结费类型")
    private String settlementType;

    @TableField("供应商名称")
    private String supplierName;

    @TableField("考勤日期")
    private LocalDate attendanceDate;

    @TableField("班次")
    private String shift;

    @TableField("用工班次")
    private String workShift;

    @TableField("班次上班时间")
    private String shiftStartTime;

    @TableField("班次下班时间")
    private String shiftEndTime;

    @TableField("上班打卡时间1")
    private String clockIn1;

    @TableField("上班打卡结果1")
    private String clockInResult1;

    @TableField("下班打卡时间1")
    private String clockOut1;

    @TableField("下班打卡结果1")
    private String clockOutResult1;

    @TableField("上班打卡时间2")
    private String clockIn2;

    @TableField("上班打卡结果2")
    private String clockInResult2;

    @TableField("下班打卡时间2")
    private String clockOut2;

    @TableField("下班打卡结果2")
    private String clockOutResult2;

    @TableField("上班打卡时间3")
    private String clockIn3;

    @TableField("上班打卡结果3")
    private String clockInResult3;

    @TableField("下班打卡时间3")
    private String clockOut3;

    @TableField("下班打卡结果3")
    private String clockOutResult3;

    @TableField("班次休息时间")
    private String shiftBreakTime;

    @TableField("工作时长")
    private String workDuration;

    @TableField("迟到次数（分钟）")
    private Integer lateMinutes;

    @TableField("早退次数")
    private Integer earlyLeaveCount;

    @TableField("早退时长（分钟）")
    private Integer earlyLeaveMinutes;

    @TableField("加班总时长（小时）")
    private Integer overtimeHours;

    @TableField("事假（小时）")
    private Integer personalLeaveHours;

    @TableField("病假（小时）")
    private Integer sickLeaveHours;

    @TableField("年假（小时）")
    private Integer annualLeaveHours;

    @TableField("因公（小时）")
    private Integer businessLeaveHours;

    @TableField("婚假（天）")
    private Integer marriageLeave;

    @TableField("产假（天）")
    private Integer maternityLeave;

    @TableField("陪产假（天）")
    private Integer paternityLeave;

    @TableField("丧假（天）")
    private Integer bereavementLeave;

    @TableField("调休（小时）")
    private Integer compLeaveHours;

    @TableField("当前工作时长（小时）")
    private Integer currentWorkHours;
}
