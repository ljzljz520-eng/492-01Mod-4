package com.scaffolding.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffolding.entity.DailySettlement;

import java.math.BigDecimal;

public interface DailySettlementService extends IService<DailySettlement> {

    Page<DailySettlement> pageQuery(Long current, Long size, Long workerId, String status, String workDateStart, String workDateEnd);

    DailySettlement generateSettlement(Long workerId, Long attendanceId, BigDecimal tempSubsidy, String remark);

    boolean confirmSettlement(Long id);

    boolean markDisputed(Long id, Long disputeId);

    boolean updateSettlementAmount(Long id, BigDecimal actualHours, BigDecimal deductionAmount, String remark);
}
