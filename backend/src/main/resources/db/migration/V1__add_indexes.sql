-- 出库单表：按仓库+时间范围的聚合查询优化
CREATE INDEX idx_outbound_wh_time ON 出库单表 (库房编码, 创建时间);

-- 出勤统计表：按仓库+日期范围的工时汇总查询优化
CREATE INDEX idx_attendance_wh_date ON 出勤统计表 (库房, 考勤日期);

-- 报价信息表：费用基线计算中的单价查询优化
CREATE INDEX idx_quote_wh_status ON 报价信息表 (库房名称, 报价状态);

-- 工作量统计信息表：按仓库+月份筛选优化
CREATE INDEX idx_workload_wh_month ON 工作量统计信息表 (库房编码, 月份);

-- 入库单表：按仓库+时间范围的入库统计查询优化
CREATE INDEX idx_inbound_wh_time ON 入库单表 (库房编码, 创建时间);
