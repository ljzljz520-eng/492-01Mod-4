package com.scaffolding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispute_evidence")
public class DisputeEvidence extends BaseEntity {

    private Long disputeId;
    private String evidenceType;
    private Long fileId;
    private String fileUrl;
    private String submitterType;
    private Long submitterId;
    private String submitterName;
    private String description;
    private Integer sortOrder;
}
