package com.scaffolding.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffolding.common.PageResult;
import com.scaffolding.common.Result;
import com.scaffolding.entity.ArbitrationRecord;
import com.scaffolding.service.ArbitrationRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/arbitration")
@Api(tags = "仲裁管理")
public class ArbitrationController {

    @Autowired
    private ArbitrationRecordService arbitrationRecordService;

    @PostMapping
    @ApiOperation("执行仲裁")
    public Result<ArbitrationRecord> doArbitration(@RequestBody Map<String, Object> params) {
        try {
            Long disputeId = Long.valueOf(params.get("disputeId").toString());
            Long arbitratorId = params.get("arbitratorId") != null ? Long.valueOf(params.get("arbitratorId").toString()) : 1L;
            String arbitratorName = params.get("arbitratorName") != null ? params.get("arbitratorName").toString() : "仲裁员";
            String arbitrationOpinion = params.get("arbitrationOpinion") != null ? params.get("arbitrationOpinion").toString() : "";
            String arbitrationResult = params.get("arbitrationResult").toString();
            BigDecimal approvedHours = params.get("approvedHours") != null ? new BigDecimal(params.get("approvedHours").toString()) : null;
            BigDecimal approvedAmount = params.get("approvedAmount") != null ? new BigDecimal(params.get("approvedAmount").toString()) : null;
            String remark = params.get("remark") != null ? params.get("remark").toString() : null;

            ArbitrationRecord record = arbitrationRecordService.doArbitration(
                    disputeId, arbitratorId, arbitratorName, arbitrationOpinion, arbitrationResult,
                    approvedHours, approvedAmount, remark);

            return Result.success("仲裁完成", record);
        } catch (Exception e) {
            log.error("仲裁失败", e);
            return Result.error("仲裁失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询仲裁记录")
    public Result<ArbitrationRecord> getById(@PathVariable Long id) {
        ArbitrationRecord record = arbitrationRecordService.getById(id);
        if (record == null) {
            return Result.error("仲裁记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/dispute/{disputeId}")
    @ApiOperation("根据争议单ID查询仲裁记录")
    public Result<ArbitrationRecord> getByDisputeId(@PathVariable Long disputeId) {
        ArbitrationRecord record = arbitrationRecordService.getByDisputeId(disputeId);
        return Result.success(record);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询仲裁记录")
    public Result<PageResult<ArbitrationRecord>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long disputeId,
            @RequestParam(required = false) String arbitrationResult) {
        Page<ArbitrationRecord> page = arbitrationRecordService.pageQuery(current, size, disputeId, arbitrationResult);

        PageResult<ArbitrationRecord> pageResult = new PageResult<>(
                page.getTotal(),
                page.getRecords(),
                page.getCurrent(),
                page.getSize()
        );
        return Result.success(pageResult);
    }
}
