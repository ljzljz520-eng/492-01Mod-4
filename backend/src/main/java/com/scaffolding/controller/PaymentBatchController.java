package com.scaffolding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffolding.common.PageResult;
import com.scaffolding.common.Result;
import com.scaffolding.entity.DailySettlement;
import com.scaffolding.entity.PaymentBatch;
import com.scaffolding.entity.WageDispute;
import com.scaffolding.entity.Worker;
import com.scaffolding.mapper.DailySettlementMapper;
import com.scaffolding.mapper.WageDisputeMapper;
import com.scaffolding.mapper.WorkerMapper;
import com.scaffolding.service.PaymentBatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/payment-batch")
@Api(tags = "打款批次管理")
public class PaymentBatchController {

    @Autowired
    private PaymentBatchService paymentBatchService;

    @Autowired
    private DailySettlementMapper dailySettlementMapper;

    @Autowired
    private WageDisputeMapper wageDisputeMapper;

    @Autowired
    private WorkerMapper workerMapper;

    @PostMapping
    @ApiOperation("创建打款批次")
    public Result<PaymentBatch> create(@RequestBody Map<String, Object> params) {
        try {
            String batchName = params.get("batchName") != null ? params.get("batchName").toString() : "日结工资打款批次";
            LocalDate paymentDate = params.get("paymentDate") != null ?
                    LocalDate.parse(params.get("paymentDate").toString()) : LocalDate.now();
            String paymentChannel = params.get("paymentChannel") != null ? params.get("paymentChannel").toString() : "bank";
            String remark = params.get("remark") != null ? params.get("remark").toString() : null;
            Long operatorId = params.get("operatorId") != null ? Long.valueOf(params.get("operatorId").toString()) : 1L;
            String operatorName = params.get("operatorName") != null ? params.get("operatorName").toString() : "财务";

            List<Long> settlementIds = new ArrayList<>();
            Object idsObj = params.get("settlementIds");
            if (idsObj != null && idsObj instanceof List) {
                List<?> idList = (List<?>) idsObj;
                for (Object idObj : idList) {
                    if (idObj instanceof Number) {
                        settlementIds.add(((Number) idObj).longValue());
                    } else if (idObj != null) {
                        settlementIds.add(Long.valueOf(idObj.toString()));
                    }
                }
            }

            PaymentBatch batch = paymentBatchService.createBatch(
                    batchName, paymentDate, paymentChannel, remark, operatorId, operatorName, settlementIds);

            return Result.success("创建打款批次成功", batch);
        } catch (Exception e) {
            log.error("创建打款批次失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("查询打款批次详情（含批次内结算单及争议原因）")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        PaymentBatch batch = paymentBatchService.getById(id);
        if (batch == null) {
            return Result.error("打款批次不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batch", batch);

        LambdaQueryWrapper<DailySettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailySettlement::getBatchId, id);
        List<DailySettlement> settlements = dailySettlementMapper.selectList(wrapper);

        List<Map<String, Object>> settlementDetails = new ArrayList<>();
        for (DailySettlement s : settlements) {
            Map<String, Object> item = new HashMap<>();
            item.put("settlement", s);

            Worker worker = workerMapper.selectById(s.getWorkerId());
            item.put("worker", worker);

            if (s.getDisputeId() != null) {
                WageDispute dispute = wageDisputeMapper.selectById(s.getDisputeId());
                item.put("dispute", dispute);
            }

            settlementDetails.add(item);
        }
        result.put("settlements", settlementDetails);

        return Result.success(result);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询打款批次")
    public Result<PageResult<PaymentBatch>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate paymentDateStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate paymentDateEnd) {
        Page<PaymentBatch> page = paymentBatchService.pageQuery(current, size, status, paymentDateStart, paymentDateEnd);

        PageResult<PaymentBatch> pageResult = new PageResult<>(
                page.getTotal(),
                page.getRecords(),
                page.getCurrent(),
                page.getSize()
        );
        return Result.success(pageResult);
    }

    @PutMapping("/{id}/mark-paid")
    @ApiOperation("标记批次已打款")
    public Result<?> markPaid(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        try {
            LocalDate actualPaymentDate = null;
            String paymentVoucher = null;
            if (params != null) {
                if (params.get("actualPaymentDate") != null) {
                    actualPaymentDate = LocalDate.parse(params.get("actualPaymentDate").toString());
                }
                if (params.get("paymentVoucher") != null) {
                    paymentVoucher = params.get("paymentVoucher").toString();
                }
            }
            paymentBatchService.markPaid(id, actualPaymentDate, paymentVoucher);
            return Result.success("标记打款成功");
        } catch (Exception e) {
            log.error("标记打款失败", e);
            return Result.error("标记失败：" + e.getMessage());
        }
    }

    @GetMapping("/available-settlements")
    @ApiOperation("获取可用于打款的已确认结算单列表（含争议原因显示）")
    public Result<List<Map<String, Object>>> getAvailableSettlements(
            @RequestParam(required = false) Long workerId,
            @RequestParam(required = false) String workDateStart,
            @RequestParam(required = false) String workDateEnd) {
        LambdaQueryWrapper<DailySettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailySettlement::getStatus, "confirmed");
        wrapper.isNull(DailySettlement::getBatchId);

        if (workerId != null) {
            wrapper.eq(DailySettlement::getWorkerId, workerId);
        }
        if (workDateStart != null && !workDateStart.isEmpty()) {
            wrapper.ge(DailySettlement::getWorkDate, LocalDate.parse(workDateStart));
        }
        if (workDateEnd != null && !workDateEnd.isEmpty()) {
            wrapper.le(DailySettlement::getWorkDate, LocalDate.parse(workDateEnd));
        }

        wrapper.orderByAsc(DailySettlement::getWorkDate);
        List<DailySettlement> settlements = dailySettlementMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (DailySettlement s : settlements) {
            boolean canPay = true;
            if (s.getDisputeId() != null) {
                WageDispute dispute = wageDisputeMapper.selectById(s.getDisputeId());
                if (dispute != null && !"approved".equals(dispute.getStatus()) && !"rejected".equals(dispute.getStatus())) {
                    canPay = false;
                }
            }
            if (!canPay) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("settlement", s);

            Worker worker = workerMapper.selectById(s.getWorkerId());
            item.put("worker", worker);

            if (s.getDisputeId() != null) {
                WageDispute dispute = wageDisputeMapper.selectById(s.getDisputeId());
                item.put("dispute", dispute);
                if (dispute != null) {
                    item.put("disputeReason", dispute.getDisputeReason());
                }
            }

            result.add(item);
        }

        return Result.success(result);
    }
}
