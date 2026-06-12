package com.scaffolding.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffolding.entity.ArbitrationRecord;

import java.math.BigDecimal;

public interface ArbitrationRecordService extends IService<ArbitrationRecord> {

    Page<ArbitrationRecord> pageQuery(Long current, Long size, Long disputeId, String arbitrationResult);

    ArbitrationRecord doArbitration(Long disputeId, Long arbitratorId, String arbitratorName,
                                     String arbitrationOpinion, String arbitrationResult,
                                     BigDecimal approvedHours, BigDecimal approvedAmount, String remark);

    ArbitrationRecord getByDisputeId(Long disputeId);
}
