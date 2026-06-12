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
@TableName("daily_settlement")
public class DailySettlement extends BaseEntity {

    private String settlementNo;
    private Long workerId;
    private Long positionId;
    private Long attendanceId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate workDate;

    private BigDecimal originalHours;
    private BigDecimal actualHours;
    private BigDecimal unitPrice;
    private BigDecimal baseAmount;
    private BigDecimal tempSubsidy;
    private BigDecimal deductionAmount;
    private BigDecimal totalAmount;
    private Long supervisorId;
    private String supervisorName;
    private String status;
    private Long disputeId;
    private Long batchId;
    private String remark;
}
