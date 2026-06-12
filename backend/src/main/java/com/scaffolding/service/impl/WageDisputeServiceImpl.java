package com.scaffolding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffolding.entity.*;
import com.scaffolding.mapper.*;
import com.scaffolding.service.DailySettlementService;
import com.scaffolding.service.DisputeEvidenceService;
import com.scaffolding.service.WageDisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class WageDisputeServiceImpl extends ServiceImpl<WageDisputeMapper, WageDispute> implements WageDisputeService {

    @Autowired
    private DailySettlementService dailySettlementService;

    @Autowired
    private DailySettlementMapper dailySettlementMapper;

    @Autowired
    private DisputeEvidenceService disputeEvidenceService;

    @Override
    public Page<WageDispute> pageQuery(Long current, Long size, Long workerId, Long settlementId, String status, String disputeType) {
        Page<WageDispute> page = new Page<>(current, size);
        LambdaQueryWrapper<WageDispute> wrapper = new LambdaQueryWrapper<>();

        if (workerId != null) {
            wrapper.eq(WageDispute::getWorkerId, workerId);
        }
        if (settlementId != null) {
            wrapper.eq(WageDispute::getSettlementId, settlementId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(WageDispute::getStatus, status);
        }
        if (StringUtils.hasText(disputeType)) {
            wrapper.eq(WageDispute::getDisputeType, disputeType);
        }

        wrapper.orderByDesc(WageDispute::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WageDispute createSupervisorDeduction(Long settlementId, Long supervisorId, String supervisorName,
                                                  BigDecimal claimedHours, BigDecimal claimedAmount,
                                                  String disputeReason, String supervisorRemark) {
        DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new RuntimeException("结算单不存在");
        }

        WageDispute dispute = new WageDispute();
        dispute.setDisputeNo("WD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        dispute.setSettlementId(settlementId);
        dispute.setWorkerId(settlement.getWorkerId());
        dispute.setDisputeType("supervisor_deduction");
        dispute.setOriginalHours(settlement.getOriginalHours());
        dispute.setClaimedHours(claimedHours);
        dispute.setOriginalAmount(settlement.getTotalAmount());
        dispute.setClaimedAmount(claimedAmount);
        dispute.setDisputeReason(disputeReason);
        dispute.setSupervisorRemark(supervisorRemark);
        dispute.setSupervisorId(supervisorId);
        dispute.setSupervisorSubmitTime(LocalDateTime.now());
        dispute.setStatus("supervisor_submitted");
        dispute.setCreateTime(LocalDateTime.now());
        dispute.setUpdateTime(LocalDateTime.now());

        this.save(dispute);
        dailySettlementService.markDisputed(settlementId, dispute.getId());

        return dispute;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WageDispute createWorkerAppeal(Long settlementId, Long workerId,
                                           BigDecimal claimedHours, BigDecimal claimedAmount,
                                           String disputeReason, String workerRemark) {
        DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new RuntimeException("结算单不存在");
        }

        WageDispute dispute = new WageDispute();
        dispute.setDisputeNo("WD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        dispute.setSettlementId(settlementId);
        dispute.setWorkerId(workerId);
        dispute.setDisputeType("worker_appeal");
        dispute.setOriginalHours(settlement.getOriginalHours());
        dispute.setClaimedHours(claimedHours);
        dispute.setOriginalAmount(settlement.getTotalAmount());
        dispute.setClaimedAmount(claimedAmount);
        dispute.setDisputeReason(disputeReason);
        dispute.setWorkerRemark(workerRemark);
        dispute.setWorkerSubmitTime(LocalDateTime.now());
        dispute.setStatus("worker_submitted");
        dispute.setCreateTime(LocalDateTime.now());
        dispute.setUpdateTime(LocalDateTime.now());

        this.save(dispute);
        dailySettlementService.markDisputed(settlementId, dispute.getId());

        return dispute;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WageDispute workerSubmit(Long disputeId, Long workerId, String workerRemark, List<DisputeEvidence> evidences) {
        WageDispute dispute = this.getById(disputeId);
        if (dispute == null) {
            throw new RuntimeException("争议单不存在");
        }

        dispute.setWorkerRemark(workerRemark);
        dispute.setWorkerSubmitTime(LocalDateTime.now());

        if ("pending".equals(dispute.getStatus())) {
            dispute.setStatus("worker_submitted");
        } else if ("supervisor_submitted".equals(dispute.getStatus())) {
            dispute.setStatus("arbitrating");
        }

        dispute.setUpdateTime(LocalDateTime.now());
        this.updateById(dispute);

        if (evidences != null && !evidences.isEmpty()) {
            for (DisputeEvidence e : evidences) {
                e.setSubmitterType("worker");
                e.setSubmitterId(workerId);
            }
            disputeEvidenceService.saveEvidences(disputeId, evidences);
        }

        return dispute;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WageDispute supervisorSubmit(Long disputeId, Long supervisorId, String supervisorName, String supervisorRemark, List<DisputeEvidence> evidences) {
        WageDispute dispute = this.getById(disputeId);
        if (dispute == null) {
            throw new RuntimeException("争议单不存在");
        }

        dispute.setSupervisorRemark(supervisorRemark);
        dispute.setSupervisorId(supervisorId);
        dispute.setSupervisorSubmitTime(LocalDateTime.now());

        if ("pending".equals(dispute.getStatus())) {
            dispute.setStatus("supervisor_submitted");
        } else if ("worker_submitted".equals(dispute.getStatus())) {
            dispute.setStatus("arbitrating");
        }

        dispute.setUpdateTime(LocalDateTime.now());
        this.updateById(dispute);

        if (evidences != null && !evidences.isEmpty()) {
            for (DisputeEvidence e : evidences) {
                e.setSubmitterType("supervisor");
                e.setSubmitterId(supervisorId);
                e.setSubmitterName(supervisorName);
            }
            disputeEvidenceService.saveEvidences(disputeId, evidences);
        }

        return dispute;
    }

    @Override
    public WageDispute getDetail(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WageDispute startArbitration(Long id) {
        WageDispute dispute = this.getById(id);
        if (dispute == null) {
            throw new RuntimeException("争议单不存在");
        }
        if ("worker_submitted".equals(dispute.getStatus()) || "supervisor_submitted".equals(dispute.getStatus())) {
            dispute.setStatus("arbitrating");
            dispute.setUpdateTime(LocalDateTime.now());
            this.updateById(dispute);
        }
        return dispute;
    }
}
