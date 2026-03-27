package com.kejie.whop.controller;

import com.kejie.whop.model.dto.EstimateRequest;
import com.kejie.whop.model.vo.EstimateResultVO;
import com.kejie.whop.model.vo.Result;
import com.kejie.whop.service.EstimateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estimate")
@RequiredArgsConstructor
public class EstimateController {

    private final EstimateService estimateService;

    @PostMapping("/calculate")
    public Result<EstimateResultVO> calculate(@Valid @RequestBody EstimateRequest request) {
        return Result.ok(estimateService.calculate(request));
    }

    @GetMapping("/defaults/{warehouseCode}")
    public Result<EstimateRequest> defaults(@PathVariable String warehouseCode) {
        return Result.ok(estimateService.getDefaults(warehouseCode));
    }
}
