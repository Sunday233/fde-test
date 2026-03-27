package com.kejie.whop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kejie.whop.mapper.AttendanceStatisticsMapper;
import com.kejie.whop.mapper.OutboundOrderMapper;
import com.kejie.whop.model.entity.AttendanceStatistics;
import com.kejie.whop.model.entity.OutboundOrder;
import com.kejie.whop.model.vo.WarehouseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final AttendanceStatisticsMapper attendanceStatisticsMapper;

    public List<WarehouseVO> list() {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("DISTINCT 库房编码, 库房名称")
                .isNotNull("库房编码")
                .ne("库房编码", "");
        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        return rows.stream().map(row -> {
            WarehouseVO vo = new WarehouseVO();
            vo.setWarehouseCode(String.valueOf(row.get("库房编码")));
            vo.setWarehouseName(String.valueOf(row.get("库房名称")));
            return vo;
        }).toList();
    }

    /**
     * 根据仓库编码获取仓库名称
     */
    public String getWarehouseName(String warehouseCode) {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("DISTINCT 库房名称")
                .eq("库房编码", warehouseCode)
                .last("LIMIT 1");
        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        if (rows.isEmpty() || rows.get(0) == null) {
            return null;
        }
        return String.valueOf(rows.get(0).get("库房名称"));
    }

    public List<String> getAvailableMonths() {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("DISTINCT DATE_FORMAT(创建时间, '%Y-%m') as month")
                .isNotNull("创建时间")
                .orderByDesc("month");
        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        return rows.stream()
                .filter(r -> r != null && r.get("month") != null)
                .map(r -> String.valueOf(r.get("month")))
                .toList();
    }

    /**
     * 获取考勤表中实际使用的仓库名称（可能和出库单表名称不同）
     */
    public String getAttendanceWarehouseName(String outboundWarehouseName) {
        if (outboundWarehouseName == null) return null;
        // 先精确匹配
        QueryWrapper<AttendanceStatistics> qw = new QueryWrapper<>();
        qw.select("DISTINCT 库房").eq("库房", outboundWarehouseName).last("LIMIT 1");
        List<Map<String, Object>> rows = attendanceStatisticsMapper.selectMaps(qw);
        if (!rows.isEmpty() && rows.get(0) != null) {
            return String.valueOf(rows.get(0).get("库房"));
        }
        // 模糊匹配：去掉字母后缀进行LIKE匹配
        String baseName = outboundWarehouseName.replaceAll("[A-Za-z]+仓$", "仓");
        if (!baseName.equals(outboundWarehouseName)) {
            QueryWrapper<AttendanceStatistics> fuzzyQw = new QueryWrapper<>();
            fuzzyQw.select("DISTINCT 库房").like("库房", baseName.replace("仓", "")).last("LIMIT 1");
            rows = attendanceStatisticsMapper.selectMaps(fuzzyQw);
            if (!rows.isEmpty() && rows.get(0) != null) {
                return String.valueOf(rows.get(0).get("库房"));
            }
        }
        return outboundWarehouseName;
    }

    /**
     * 获取仓库(库房编码)下所有工厂编码列表（工作量统计表使用工厂编码）
     */
    public List<String> getFactoryCodesForWarehouse(String warehouseCode) {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("DISTINCT 工厂编码")
                .eq("库房编码", warehouseCode)
                .isNotNull("工厂编码")
                .ne("工厂编码", "");
        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        return rows.stream()
                .filter(r -> r != null && r.get("工厂编码") != null)
                .map(r -> String.valueOf(r.get("工厂编码")))
                .collect(Collectors.toList());
    }
}
