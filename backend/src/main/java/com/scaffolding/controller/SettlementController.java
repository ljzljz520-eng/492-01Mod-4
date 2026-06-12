package com.scaffolding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffolding.common.PageResult;
import com.scaffolding.common.Result;
import com.scaffolding.entity.*;
import com.scaffolding.mapper.AttendanceMapper;
import com.scaffolding.mapper.PositionMapper;
import com.scaffolding.mapper.WageDisputeMapper;
import com.scaffolding.mapper.WorkerMapper;
import com.scaffolding.service.ArbitrationRecordService;
import com.scaffolding.service.DailySettlementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/settlement")
@Api(tags = "日结工资单管理")
public class SettlementController {

    @Autowired
    private DailySettlementService dailySettlementService;

    @Autowired
    private WorkerMapper workerMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private WageDisputeMapper wageDisputeMapper;

    @Autowired
    private ArbitrationRecordService arbitrationRecordService;

    @PostMapping
    @ApiOperation("生成日结工资单")
    public Result<DailySettlement> generate(@RequestBody Map<String, Object> params) {
        try {
            Long workerId = Long.valueOf(params.get("workerId").toString());
            Long attendanceId = Long.valueOf(params.get("attendanceId").toString());
            BigDecimal tempSubsidy = params.get("tempSubsidy") != null ?
                    new BigDecimal(params.get("tempSubsidy").toString()) : BigDecimal.ZERO;
            String remark = params.get("remark") != null ? params.get("remark").toString() : null;
            DailySettlement settlement = dailySettlementService.generateSettlement(workerId, attendanceId, tempSubsidy, remark);
            return Result.success("生成成功", settlement);
        } catch (Exception e) {
            log.error("生成日结工资单失败", e);
            return Result.error("生成失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询日结工资单（含详情和关联争议）")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        DailySettlement settlement = dailySettlementService.getById(id);
        if (settlement == null) {
            return Result.error("结算单不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("settlement", settlement);

        Worker worker = workerMapper.selectById(settlement.getWorkerId());
        result.put("worker", worker);

        Position position = positionMapper.selectById(settlement.getPositionId());
        result.put("position", position);

        if (settlement.getAttendanceId() != null) {
            Attendance attendance = attendanceMapper.selectById(settlement.getAttendanceId());
            result.put("attendance", attendance);
        }

        if (settlement.getDisputeId() != null) {
            WageDispute dispute = wageDisputeMapper.selectById(settlement.getDisputeId());
            result.put("dispute", dispute);
            if (dispute != null && dispute.getArbitrationId() != null) {
                ArbitrationRecord arbitration = arbitrationRecordService.getById(dispute.getArbitrationId());
                result.put("arbitration", arbitration);
            }
        }

        return Result.success(result);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询日结工资单")
    public Result<PageResult<DailySettlement>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long workerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workDateStart,
            @RequestParam(required = false) String workDateEnd) {
        Page<DailySettlement> page = dailySettlementService.pageQuery(current, size, workerId, status, workDateStart, workDateEnd);

        PageResult<DailySettlement> pageResult = new PageResult<>(
                page.getTotal(),
                page.getRecords(),
                page.getCurrent(),
                page.getSize()
        );
        return Result.success(pageResult);
    }

    @GetMapping("/page/detail")
    @ApiOperation("分页查询日结工资单（含工人信息和争议原因，供财务查看）")
    public Result<PageResult<Map<String, Object>>> pageWithDetail(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long workerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workDateStart,
            @RequestParam(required = false) String workDateEnd,
            @RequestParam(required = false) Long batchId) {
        Page<DailySettlement> page = new Page<>(current, size);
        LambdaQueryWrapper<DailySettlement> wrapper = new LambdaQueryWrapper<>();

        if (workerId != null) {
            wrapper.eq(DailySettlement::getWorkerId, workerId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(DailySettlement::getStatus, status);
        }
        if (workDateStart != null && !workDateStart.isEmpty()) {
            wrapper.ge(DailySettlement::getWorkDate, java.time.LocalDate.parse(workDateStart));
        }
        if (workDateEnd != null && !workDateEnd.isEmpty()) {
            wrapper.le(DailySettlement::getWorkDate, java.time.LocalDate.parse(workDateEnd));
        }
        if (batchId != null) {
            wrapper.eq(DailySettlement::getBatchId, batchId);
        }

        wrapper.orderByDesc(DailySettlement::getCreateTime);
        dailySettlementService.page(page, wrapper);

        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (DailySettlement s : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("settlement", s);

            Worker worker = workerMapper.selectById(s.getWorkerId());
            item.put("worker", worker);

            Position position = positionMapper.selectById(s.getPositionId());
            item.put("position", position);

            if (s.getDisputeId() != null) {
                WageDispute dispute = wageDisputeMapper.selectById(s.getDisputeId());
                item.put("dispute", dispute);
                if (dispute != null && dispute.getArbitrationId() != null) {
                    ArbitrationRecord arbitration = arbitrationRecordService.getById(dispute.getArbitrationId());
                    item.put("arbitration", arbitration);
                }
            }

            records.add(item);
        }

        PageResult<Map<String, Object>> pageResult = new PageResult<>(
                page.getTotal(),
                records,
                page.getCurrent(),
                page.getSize()
        );
        return Result.success(pageResult);
    }

    @PutMapping("/{id}/confirm")
    @ApiOperation("确认结算单")
    public Result<?> confirm(@PathVariable Long id) {
        try {
            dailySettlementService.confirmSettlement(id);
            return Result.success("确认成功");
        } catch (Exception e) {
            log.error("确认结算单失败", e);
            return Result.error("确认失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/amount")
    @ApiOperation("调整结算金额")
    public Result<?> updateAmount(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            BigDecimal actualHours = params.get("actualHours") != null ? new BigDecimal(params.get("actualHours").toString()) : null;
            BigDecimal deductionAmount = params.get("deductionAmount") != null ? new BigDecimal(params.get("deductionAmount").toString()) : null;
            String remark = params.get("remark") != null ? params.get("remark").toString() : null;
            dailySettlementService.updateSettlementAmount(id, actualHours, deductionAmount, remark);
            return Result.success("调整成功");
        } catch (Exception e) {
            log.error("调整结算金额失败", e);
            return Result.error("调整失败：" + e.getMessage());
        }
    }
}
