package com.scaffolding.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffolding.entity.PaymentBatch;

import java.time.LocalDate;
import java.util.List;

public interface PaymentBatchService extends IService<PaymentBatch> {

    Page<PaymentBatch> pageQuery(Long current, Long size, String status, LocalDate paymentDateStart, LocalDate paymentDateEnd);

    PaymentBatch createBatch(String batchName, LocalDate paymentDate, String paymentChannel, String remark,
                              Long operatorId, String operatorName, List<Long> settlementIds);

    boolean markPaid(Long id, LocalDate actualPaymentDate, String paymentVoucher);
}
