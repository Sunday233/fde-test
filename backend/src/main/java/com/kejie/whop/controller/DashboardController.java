package com.kejie.whop.controller;

import com.kejie.whop.model.vo.DashboardOverviewVO;
import com.kejie.whop.model.vo.Result;
import com.kejie.whop.model.vo.TrendDataVO;
import com.kejie.whop.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String month) {
        return Result.ok(dashboardService.getOverview(warehouseCode, month));
    }

    @GetMapping("/trend")
    public Result<List<TrendDataVO>> trend(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestParam(required = false, defaultValue = "outbound_orders") String type) {
        return Result.ok(dashboardService.getTrend(warehouseCode, startMonth, endMonth, type));
    }
}
