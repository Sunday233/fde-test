package com.kejie.whop.controller;

import com.kejie.whop.model.vo.CompareResultVO;
import com.kejie.whop.model.vo.DailyDetailVO;
import com.kejie.whop.model.vo.MonthlyBaselineVO;
import com.kejie.whop.model.vo.OperationFeeDetailVO;
import com.kejie.whop.model.vo.PageResult;
import com.kejie.whop.model.vo.Result;
import com.kejie.whop.model.vo.WarehouseDetailVO;
import com.kejie.whop.service.BaselineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/baseline")
@RequiredArgsConstructor
public class BaselineController {

    private final BaselineService baselineService;

    @GetMapping("/latestMonth")
    public Result<String> latestMonth() {
        return Result.ok(baselineService.getLatestMonth());
    }

    @GetMapping("/monthly")
    public Result<?> monthly(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        String latest = baselineService.getLatestMonth();
        if (endMonth == null) endMonth = latest;
        if (startMonth == null) startMonth = YearMonth.parse(endMonth).minusMonths(11).toString();
        List<MonthlyBaselineVO> all = baselineService.getMonthlyBaseline(warehouseCode, startMonth, endMonth);
        if (page != null && size != null) {
            int fromIndex = Math.min((page - 1) * size, all.size());
            int toIndex = Math.min(fromIndex + size, all.size());
            return Result.ok(PageResult.of(all.subList(fromIndex, toIndex), all.size(), page, size));
        }
        return Result.ok(all);
    }

    @GetMapping("/warehouse/{warehouseCode}")
    public Result<WarehouseDetailVO> warehouseDetail(
            @PathVariable String warehouseCode,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year == null || month == null) {
            String latest = baselineService.getLatestMonth();
            String[] parts = latest.split("-");
            if (year == null) year = Integer.parseInt(parts[0]);
            if (month == null) month = Integer.parseInt(parts[1]);
        }
        return Result.ok(baselineService.getWarehouseDetail(warehouseCode, year, month));
    }

    @GetMapping("/compare")
    public Result<List<CompareResultVO>> compare(
            @RequestParam String codes,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestParam(defaultValue = "month") String granularity) {
        String latest = baselineService.getLatestMonth();
        if (endMonth == null) endMonth = latest;
        if (startMonth == null) startMonth = YearMonth.parse(endMonth).minusMonths(11).toString();
        return Result.ok(baselineService.compare(codes, startMonth, endMonth, granularity));
    }

    @GetMapping("/daily-detail")
    public Result<List<DailyDetailVO>> dailyDetail(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        String latest = baselineService.getLatestMonth();
        if (endMonth == null) endMonth = latest;
        if (startMonth == null) startMonth = YearMonth.parse(endMonth).minusMonths(11).toString();
        return Result.ok(baselineService.getDailyDetail(warehouseCode, startMonth, endMonth));
    }

    @GetMapping({"/operation-fee-detail", "/operationFeeDetail"})
    public Result<List<OperationFeeDetailVO>> operationFeeDetail(
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth,
            @RequestParam(defaultValue = "month") String dimension) {
        String latest = baselineService.getLatestMonth();
        if (endMonth == null) endMonth = latest;
        if (startMonth == null) startMonth = YearMonth.parse(endMonth).minusMonths(11).toString();
        return Result.ok(baselineService.getOperationFeeDetail(warehouseCode, startMonth, endMonth, dimension));
    }
}
