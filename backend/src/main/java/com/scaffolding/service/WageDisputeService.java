package com.scaffolding.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffolding.entity.DisputeEvidence;
import com.scaffolding.entity.WageDispute;

import java.math.BigDecimal;
import java.util.List;

public interface WageDisputeService extends IService<WageDispute> {

    Page<WageDispute> pageQuery(Long current, Long size, Long workerId, Long settlementId, String status, String disputeType);

    WageDispute createSupervisorDeduction(Long settlementId, Long supervisorId, String supervisorName,
                                           BigDecimal claimedHours, BigDecimal claimedAmount,
                                           String disputeReason, String supervisorRemark);

    WageDispute createWorkerAppeal(Long settlementId, Long workerId,
                                    BigDecimal claimedHours, BigDecimal claimedAmount,
                                    String disputeReason, String workerRemark);

    WageDispute workerSubmit(Long disputeId, Long workerId, String workerRemark, List<DisputeEvidence> evidences);

    WageDispute supervisorSubmit(Long disputeId, Long supervisorId, String supervisorName, String supervisorRemark, List<DisputeEvidence> evidences);

    WageDispute getDetail(Long id);

    WageDispute startArbitration(Long id);
}
