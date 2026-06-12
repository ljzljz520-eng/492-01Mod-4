package com.scaffolding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffolding.entity.DailySettlement;
import com.scaffolding.entity.PaymentBatch;
import com.scaffolding.entity.WageDispute;
import com.scaffolding.mapper.DailySettlementMapper;
import com.scaffolding.mapper.PaymentBatchMapper;
import com.scaffolding.mapper.WageDisputeMapper;
import com.scaffolding.service.PaymentBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentBatchServiceImpl extends ServiceImpl<PaymentBatchMapper, PaymentBatch> implements PaymentBatchService {

    @Autowired
    private DailySettlementMapper dailySettlementMapper;

    @Autowired
    private WageDisputeMapper wageDisputeMapper;

    @Override
    public Page<PaymentBatch> pageQuery(Long current, Long size, String status, LocalDate paymentDateStart, LocalDate paymentDateEnd) {
        Page<PaymentBatch> page = new Page<>(current, size);
        LambdaQueryWrapper<PaymentBatch> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(PaymentBatch::getStatus, status);
        }
        if (paymentDateStart != null) {
            wrapper.ge(PaymentBatch::getPaymentDate, paymentDateStart);
        }
        if (paymentDateEnd != null) {
            wrapper.le(PaymentBatch::getPaymentDate, paymentDateEnd);
        }

        wrapper.orderByDesc(PaymentBatch::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentBatch createBatch(String batchName, LocalDate paymentDate, String paymentChannel, String remark,
                                     Long operatorId, String operatorName, List<Long> settlementIds) {
        if (settlementIds == null || settlementIds.isEmpty()) {
            throw new RuntimeException("请选择要打款的结算单");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int count = 0;

        for (Long settlementId : settlementIds) {
            DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
            if (settlement == null) {
                throw new RuntimeException("结算单不存在: " + settlementId);
            }
            if (!"confirmed".equals(settlement.getStatus())) {
                throw new RuntimeException("结算单[" + settlement.getSettlementNo() + "]状态不允许打款，当前状态: " + settlement.getStatus());
            }
            if (settlement.getDisputeId() != null) {
                WageDispute dispute = wageDisputeMapper.selectById(settlement.getDisputeId());
                if (dispute != null && !"approved".equals(dispute.getStatus()) && !"rejected".equals(dispute.getStatus())) {
                    throw new RuntimeException("结算单[" + settlement.getSettlementNo() + "]关联的争议单尚未完成仲裁，当前状态: " + dispute.getStatus());
                }
            }
            totalAmount = totalAmount.add(settlement.getTotalAmount());
            count++;
        }

        PaymentBatch batch = new PaymentBatch();
        batch.setBatchNo("PB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        batch.setBatchName(batchName);
        batch.setTotalCount(count);
        batch.setTotalAmount(totalAmount);
        batch.setPaymentDate(paymentDate);
        batch.setPaymentChannel(paymentChannel);
        batch.setOperatorId(operatorId);
        batch.setOperatorName(operatorName);
        batch.setStatus("pending");
        batch.setRemark(remark);
        batch.setCreateTime(LocalDateTime.now());
        batch.setUpdateTime(LocalDateTime.now());
        this.save(batch);

        for (Long settlementId : settlementIds) {
            DailySettlement settlement = dailySettlementMapper.selectById(settlementId);
            settlement.setBatchId(batch.getId());
            settlement.setUpdateTime(LocalDateTime.now());
            dailySettlementMapper.updateById(settlement);
        }

        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(Long id, LocalDate actualPaymentDate, String paymentVoucher) {
        PaymentBatch batch = this.getById(id);
        if (batch == null) {
            throw new RuntimeException("打款批次不存在");
        }

        batch.setStatus("completed");
        batch.setActualPaymentDate(actualPaymentDate != null ? actualPaymentDate : LocalDate.now());
        batch.setPaymentVoucher(paymentVoucher);
        batch.setUpdateTime(LocalDateTime.now());
        this.updateById(batch);

        LambdaQueryWrapper<DailySettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailySettlement::getBatchId, id);
        List<DailySettlement> settlements = dailySettlementMapper.selectList(wrapper);
        for (DailySettlement s : settlements) {
            s.setStatus("paid");
            s.setUpdateTime(LocalDateTime.now());
            dailySettlementMapper.updateById(s);
        }

        return true;
    }
}
