package com.kejie.whop.controller;

import com.kejie.whop.model.vo.CorrelationMatrixVO;
import com.kejie.whop.model.vo.FactorRankVO;
import com.kejie.whop.model.vo.Result;
import com.kejie.whop.model.vo.ScatterPointVO;
import com.kejie.whop.service.ImpactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/impact")
@RequiredArgsConstructor
public class ImpactController {

    private final ImpactService impactService;

    @GetMapping("/factors")
    public Result<List<FactorRankVO>> factors(
            @RequestParam(required = false) String warehouseCode) {
        return Result.ok(impactService.getFactors(warehouseCode));
    }

    @GetMapping("/correlation")
    public Result<CorrelationMatrixVO> correlation(
            @RequestParam(required = false) String warehouseCode) {
        return Result.ok(impactService.getCorrelation(warehouseCode));
    }

    @GetMapping("/scatter")
    public Result<List<ScatterPointVO>> scatter(
            @RequestParam String warehouseCode,
            @RequestParam String factor) {
        return Result.ok(impactService.getScatter(warehouseCode, factor));
    }
}
