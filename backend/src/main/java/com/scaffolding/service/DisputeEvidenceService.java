package com.scaffolding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffolding.entity.DisputeEvidence;

import java.util.List;

public interface DisputeEvidenceService extends IService<DisputeEvidence> {

    List<DisputeEvidence> listByDisputeId(Long disputeId);

    boolean saveEvidences(Long disputeId, List<DisputeEvidence> evidences);
}
