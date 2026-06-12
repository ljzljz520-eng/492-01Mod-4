package com.scaffolding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wage_dispute")
public class WageDispute extends BaseEntity {

    private String disputeNo;
    private Long settlementId;
    private Long workerId;
    private String disputeType;
    private BigDecimal originalHours;
    private BigDecimal claimedHours;
    private BigDecimal originalAmount;
    private BigDecimal claimedAmount;
    private String disputeReason;
    private String workerRemark;
    private String supervisorRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime workerSubmitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime supervisorSubmitTime;

    private Long supervisorId;
    private String status;
    private Long arbitrationId;
    private BigDecimal finalHours;
    private BigDecimal finalAmount;
}
