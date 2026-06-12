package com.scaffolding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_batch")
public class PaymentBatch extends BaseEntity {

    private String batchNo;
    private String batchName;
    private Integer totalCount;
    private BigDecimal totalAmount;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate paymentDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate actualPaymentDate;

    private Long operatorId;
    private String operatorName;
    private String status;
    private String paymentChannel;
    private String paymentVoucher;
    private String remark;
}
