package com.scaffolding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffolding.entity.ArbitrationRecord;
import com.scaffolding.entity.DailySettlement;
import com.scaffolding.entity.WageDispute;
import com.scaffolding.mapper.ArbitrationRecordMapper;
import com.scaffolding.mapper.DailySettlementMapper;
import com.scaffolding.mapper.WageDisputeMapper;
import com.scaffolding.service.ArbitrationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ArbitrationRecordServiceImpl extends ServiceImpl<ArbitrationRecordMapper, ArbitrationRecord> implements ArbitrationRecordService {

    @Autowired
    private WageDisputeMapper wageDisputeMapper;

    @Autowired
    private DailySettlementMapper dailySettlementMapper;

    @Override
    public Page<ArbitrationRecord> pageQuery(Long current, Long size, Long disputeId, String arbitrationResult) {
        Page<ArbitrationRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<ArbitrationRecord> wrapper = new LambdaQueryWrapper<>();

        if (disputeId != null) {
            wrapper.eq(ArbitrationRecord::getDisputeId, disputeId);
        }
        if (StringUtils.hasText(arbitrationResult)) {
            wrapper.eq(ArbitrationRecord::getArbitrationResult, arbitrationResult);
        }

        wrapper.orderByDesc(ArbitrationRecord::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArbitrationRecord doArbitration(Long disputeId, Long arbitratorId, String arbitratorName,
                                            String arbitrationOpinion, String arbitrationResult,
                                            BigDecimal approvedHours, BigDecimal approvedAmount, String remark) {
        WageDispute dispute = wageDisputeMapper.selectById(disputeId);
        if (dispute == null) {
            throw new RuntimeException("争议单不存在");
        }
        if (!"arbitrating".equals(dispute.getStatus())) {
            throw new RuntimeException("当前状态不允许仲裁");
        }

        ArbitrationRecord record = new ArbitrationRecord();
        record.setDisputeId(disputeId);
        record.setArbitratorId(arbitratorId);
        record.setArbitratorName(arbitratorName);
        record.setArbitrationOpinion(arbitrationOpinion);
        record.setArbitrationResult(arbitrationResult);
        record.setApprovedHours(approvedHours);
        record.setApprovedAmount(approvedAmount);
        record.setArbitrationTime(LocalDateTime.now());
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        this.save(record);

        if ("approved".equals(arbitrationResult) || "partial".equals(arbitrationResult)) {
            dispute.setStatus("approved");
            dispute.setFinalHours(approvedHours);
            dispute.setFinalAmount(approvedAmount);
        } else if ("rejected".equals(arbitrationResult)) {
            dispute.setStatus("rejected");
            dispute.setFinalHours(dispute.getOriginalHours());
            dispute.setFinalAmount(dispute.getOriginalAmount());
        }
        dispute.setArbitrationId(record.getId());
        dispute.setUpdateTime(LocalDateTime.now());
        wageDisputeMapper.updateById(dispute);

        DailySettlement settlement = dailySettlementMapper.selectById(dispute.getSettlementId());
        if (settlement != null) {
            if ("approved".equals(arbitrationResult) || "partial".equals(arbitrationResult)) {
                settlement.setActualHours(approvedHours);
                settlement.setTotalAmount(approvedAmount);
                settlement.setStatus("confirmed");
                settlement.setRemark("仲裁" + ("approved".equals(arbitrationResult) ? "通过" : "部分支持") + "，原金额：" + dispute.getOriginalAmount() + "，争议原因：" + dispute.getDisputeReason());
            } else {
                settlement.setStatus("confirmed");
                settlement.setRemark("仲裁驳回，按原始金额结算，争议原因：" + dispute.getDisputeReason());
            }
            settlement.setUpdateTime(LocalDateTime.now());
            dailySettlementMapper.updateById(settlement);
        }

        return record;
    }

    @Override
    public ArbitrationRecord getByDisputeId(Long disputeId) {
        LambdaQueryWrapper<ArbitrationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArbitrationRecord::getDisputeId, disputeId);
        wrapper.orderByDesc(ArbitrationRecord::getCreateTime);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }
}
