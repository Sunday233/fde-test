package com.kejie.whop.controller;

import com.kejie.whop.model.vo.Result;
import com.kejie.whop.model.vo.WarehouseVO;
import com.kejie.whop.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/warehouses")
    public Result<List<WarehouseVO>> list() {
        return Result.ok(warehouseService.list());
    }

    @GetMapping("/available-months")
    public Result<List<String>> availableMonths() {
        return Result.ok(warehouseService.getAvailableMonths());
    }
}
