package com.scaffolding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("worker")
public class Worker extends BaseEntity {

    private String workerNo;
    private String workerName;
    private String idCard;
    private String phone;
    private Long positionId;
    private String bankCard;
    private String bankName;
    private Integer status;
    private String remark;
}
