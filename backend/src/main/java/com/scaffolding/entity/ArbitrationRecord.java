package com.scaffolding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("arbitration_record")
public class ArbitrationRecord extends BaseEntity {

    private Long disputeId;
    private Long arbitratorId;
    private String arbitratorName;
    private String arbitrationOpinion;
    private String arbitrationResult;
    private BigDecimal approvedHours;
    private BigDecimal approvedAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime arbitrationTime;

    private String remark;
}
