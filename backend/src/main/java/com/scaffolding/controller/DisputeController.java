package com.scaffolding.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffolding.common.PageResult;
import com.scaffolding.common.Result;
import com.scaffolding.entity.ArbitrationRecord;
import com.scaffolding.entity.DisputeEvidence;
import com.scaffolding.entity.WageDispute;
import com.scaffolding.service.ArbitrationRecordService;
import com.scaffolding.service.DisputeEvidenceService;
import com.scaffolding.service.WageDisputeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dispute")
@Api(tags = "工资争议管理")
public class DisputeController {

    @Autowired
    private WageDisputeService wageDisputeService;

    @Autowired
    private DisputeEvidenceService disputeEvidenceService;

    @Autowired
    private ArbitrationRecordService arbitrationRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/supervisor-deduction")
    @ApiOperation("主管发起扣时争议")
    public Result<WageDispute> createSupervisorDeduction(@RequestBody Map<String, Object> params) {
        try {
            Long settlementId = Long.valueOf(params.get("settlementId").toString());
            Long supervisorId = params.get("supervisorId") != null ? Long.valueOf(params.get("supervisorId").toString()) : 1L;
            String supervisorName = params.get("supervisorName") != null ? params.get("supervisorName").toString() : "主管";
            BigDecimal claimedHours = new BigDecimal(params.get("claimedHours").toString());
            BigDecimal claimedAmount = new BigDecimal(params.get("claimedAmount").toString());
            String disputeReason = params.get("disputeReason") != null ? params.get("disputeReason").toString() : "";
            String supervisorRemark = params.get("supervisorRemark") != null ? params.get("supervisorRemark").toString() : "";

            WageDispute dispute = wageDisputeService.createSupervisorDeduction(
                    settlementId, supervisorId, supervisorName, claimedHours, claimedAmount, disputeReason, supervisorRemark);

            List<DisputeEvidence> evidences = null;
            if (params.get("evidences") != null) {
                evidences = objectMapper.convertValue(params.get("evidences"), new TypeReference<List<DisputeEvidence>>() {});
            }
            if (evidences != null && !evidences.isEmpty()) {
                for (DisputeEvidence e : evidences) {
                    e.setSubmitterType("supervisor");
                    e.setSubmitterId(supervisorId);
                    e.setSubmitterName(supervisorName);
                }
                disputeEvidenceService.saveEvidences(dispute.getId(), evidences);
            }

            return Result.success("发起扣时争议成功", dispute);
        } catch (Exception e) {
            log.error("发起扣时争议失败", e);
            return Result.error("发起失败：" + e.getMessage());
        }
    }

    @PostMapping("/worker-appeal")
    @ApiOperation("工人发起申诉争议")
    public Result<WageDispute> createWorkerAppeal(@RequestBody Map<String, Object> params) {
        try {
            Long settlementId = Long.valueOf(params.get("settlementId").toString());
            Long workerId = Long.valueOf(params.get("workerId").toString());
            BigDecimal claimedHours = new BigDecimal(params.get("claimedHours").toString());
            BigDecimal claimedAmount = new BigDecimal(params.get("claimedAmount").toString());
            String disputeReason = params.get("disputeReason") != null ? params.get("disputeReason").toString() : "";
            String workerRemark = params.get("workerRemark") != null ? params.get("workerRemark").toString() : "";

            WageDispute dispute = wageDisputeService.createWorkerAppeal(
                    settlementId, workerId, claimedHours, claimedAmount, disputeReason, workerRemark);

            List<DisputeEvidence> evidences = null;
            if (params.get("evidences") != null) {
                evidences = objectMapper.convertValue(params.get("evidences"), new TypeReference<List<DisputeEvidence>>() {});
            }
            if (evidences != null && !evidences.isEmpty()) {
                for (DisputeEvidence e : evidences) {
                    e.setSubmitterType("worker");
                    e.setSubmitterId(workerId);
                }
                disputeEvidenceService.saveEvidences(dispute.getId(), evidences);
            }

            return Result.success("发起申诉成功", dispute);
        } catch (Exception e) {
            log.error("发起申诉失败", e);
            return Result.error("发起失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/worker-submit")
    @ApiOperation("工人补充提交证据和说明")
    public Result<WageDispute> workerSubmit(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            Long workerId = Long.valueOf(params.get("workerId").toString());
            String workerRemark = params.get("workerRemark") != null ? params.get("workerRemark").toString() : "";

            List<DisputeEvidence> evidences = null;
            if (params.get("evidences") != null) {
                evidences = objectMapper.convertValue(params.get("evidences"), new TypeReference<List<DisputeEvidence>>() {});
            }

            WageDispute dispute = wageDisputeService.workerSubmit(id, workerId, workerRemark, evidences);
            return Result.success("提交成功", dispute);
        } catch (Exception e) {
            log.error("工人提交失败", e);
            return Result.error("提交失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/supervisor-submit")
    @ApiOperation("主管补充提交证据和说明")
    public Result<WageDispute> supervisorSubmit(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            Long supervisorId = params.get("supervisorId") != null ? Long.valueOf(params.get("supervisorId").toString()) : 1L;
            String supervisorName = params.get("supervisorName") != null ? params.get("supervisorName").toString() : "主管";
            String supervisorRemark = params.get("supervisorRemark") != null ? params.get("supervisorRemark").toString() : "";

            List<DisputeEvidence> evidences = null;
            if (params.get("evidences") != null) {
                evidences = objectMapper.convertValue(params.get("evidences"), new TypeReference<List<DisputeEvidence>>() {});
            }

            WageDispute dispute = wageDisputeService.supervisorSubmit(id, supervisorId, supervisorName, supervisorRemark, evidences);
            return Result.success("提交成功", dispute);
        } catch (Exception e) {
            log.error("主管提交失败", e);
            return Result.error("提交失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("查询争议单详情（含证据和仲裁记录）")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        WageDispute dispute = wageDisputeService.getDetail(id);
        if (dispute == null) {
            return Result.error("争议单不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dispute", dispute);

        List<DisputeEvidence> evidences = disputeEvidenceService.listByDisputeId(id);
        result.put("evidences", evidences);

        ArbitrationRecord arbitration = arbitrationRecordService.getByDisputeId(id);
        result.put("arbitration", arbitration);

        return Result.success(result);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询争议单")
    public Result<PageResult<WageDispute>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long workerId,
            @RequestParam(required = false) Long settlementId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String disputeType) {
        Page<WageDispute> page = wageDisputeService.pageQuery(current, size, workerId, settlementId, status, disputeType);

        PageResult<WageDispute> pageResult = new PageResult<>(
                page.getTotal(),
                page.getRecords(),
                page.getCurrent(),
                page.getSize()
        );
        return Result.success(pageResult);
    }

    @PutMapping("/{id}/start-arbitration")
    @ApiOperation("启动仲裁（双方都提交后或手动启动）")
    public Result<WageDispute> startArbitration(@PathVariable Long id) {
        try {
            WageDispute dispute = wageDisputeService.startArbitration(id);
            return Result.success("已启动仲裁", dispute);
        } catch (Exception e) {
            log.error("启动仲裁失败", e);
            return Result.error("启动失败：" + e.getMessage());
        }
    }
}
