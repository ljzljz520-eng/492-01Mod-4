package com.scaffolding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffolding.entity.*;
import com.scaffolding.mapper.*;
import com.scaffolding.service.DailySettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class DailySettlementServiceImpl extends ServiceImpl<DailySettlementMapper, DailySettlement> implements DailySettlementService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private WorkerMapper workerMapper;

    @Autowired
    private PositionMapper positionMapper;

    @Override
    public Page<DailySettlement> pageQuery(Long current, Long size, Long workerId, String status, String workDateStart, String workDateEnd) {
        Page<DailySettlement> page = new Page<>(current, size);
        LambdaQueryWrapper<DailySettlement> wrapper = new LambdaQueryWrapper<>();

        if (workerId != null) {
            wrapper.eq(DailySettlement::getWorkerId, workerId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(DailySettlement::getStatus, status);
        }
        if (StringUtils.hasText(workDateStart)) {
            wrapper.ge(DailySettlement::getWorkDate, LocalDate.parse(workDateStart));
        }
        if (StringUtils.hasText(workDateEnd)) {
            wrapper.le(DailySettlement::getWorkDate, LocalDate.parse(workDateEnd));
        }

        wrapper.orderByDesc(DailySettlement::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailySettlement generateSettlement(Long workerId, Long attendanceId, BigDecimal tempSubsidy, String remark) {
        Worker worker = workerMapper.selectById(workerId);
        if (worker == null) {
            throw new RuntimeException("工人不存在");
        }
        Attendance attendance = attendanceMapper.selectById(attendanceId);
        if (attendance == null) {
            throw new RuntimeException("打卡记录不存在");
        }
        Position position = positionMapper.selectById(worker.getPositionId());
        if (position == null) {
            throw new RuntimeException("岗位不存在");
        }

        BigDecimal originalHours = attendance.getOriginalHours();
        BigDecimal unitPrice = position.getUnitPrice();
        BigDecimal baseAmount = originalHours.multiply(unitPrice);
        BigDecimal subsidy = tempSubsidy != null ? tempSubsidy : BigDecimal.ZERO;
        BigDecimal totalAmount = baseAmount.add(subsidy);

        DailySettlement settlement = new DailySettlement();
        settlement.setSettlementNo("DS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        settlement.setWorkerId(workerId);
        settlement.setPositionId(worker.getPositionId());
        settlement.setAttendanceId(attendanceId);
        settlement.setWorkDate(attendance.getWorkDate());
        settlement.setOriginalHours(originalHours);
        settlement.setActualHours(originalHours);
        settlement.setUnitPrice(unitPrice);
        settlement.setBaseAmount(baseAmount);
        settlement.setTempSubsidy(subsidy);
        settlement.setDeductionAmount(BigDecimal.ZERO);
        settlement.setTotalAmount(totalAmount);
        settlement.setStatus("pending");
        settlement.setRemark(remark);
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());

        this.save(settlement);
        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmSettlement(Long id) {
        DailySettlement settlement = this.getById(id);
        if (settlement == null) {
            throw new RuntimeException("结算单不存在");
        }
        if ("disputed".equals(settlement.getStatus())) {
            throw new RuntimeException("该结算单存在争议，请先完成争议处理");
        }
        if (!"pending".equals(settlement.getStatus())) {
            throw new RuntimeException("当前状态无法确认");
        }
        settlement.setStatus("confirmed");
        settlement.setUpdateTime(LocalDateTime.now());
        return this.updateById(settlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markDisputed(Long id, Long disputeId) {
        DailySettlement settlement = this.getById(id);
        if (settlement == null) {
            throw new RuntimeException("结算单不存在");
        }
        settlement.setStatus("disputed");
        settlement.setDisputeId(disputeId);
        settlement.setUpdateTime(LocalDateTime.now());
        return this.updateById(settlement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSettlementAmount(Long id, BigDecimal actualHours, BigDecimal deductionAmount, String remark) {
        DailySettlement settlement = this.getById(id);
        if (settlement == null) {
            throw new RuntimeException("结算单不存在");
        }
        if (actualHours != null) {
            settlement.setActualHours(actualHours);
        }
        if (deductionAmount != null) {
            settlement.setDeductionAmount(deductionAmount);
        }
        BigDecimal baseAmount = settlement.getActualHours().multiply(settlement.getUnitPrice());
        settlement.setBaseAmount(baseAmount);
        settlement.setTotalAmount(baseAmount.add(settlement.getTempSubsidy()).subtract(settlement.getDeductionAmount()));
        if (remark != null) {
            settlement.setRemark(remark);
        }
        settlement.setUpdateTime(LocalDateTime.now());
        return this.updateById(settlement);
    }
}
