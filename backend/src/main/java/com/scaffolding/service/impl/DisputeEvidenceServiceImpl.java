package com.scaffolding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffolding.entity.DisputeEvidence;
import com.scaffolding.mapper.DisputeEvidenceMapper;
import com.scaffolding.service.DisputeEvidenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisputeEvidenceServiceImpl extends ServiceImpl<DisputeEvidenceMapper, DisputeEvidence> implements DisputeEvidenceService {

    @Override
    public List<DisputeEvidence> listByDisputeId(Long disputeId) {
        LambdaQueryWrapper<DisputeEvidence> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DisputeEvidence::getDisputeId, disputeId);
        wrapper.orderByAsc(DisputeEvidence::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEvidences(Long disputeId, List<DisputeEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return true;
        }
        int sortOrder = 0;
        for (DisputeEvidence evidence : evidences) {
            evidence.setDisputeId(disputeId);
            evidence.setSortOrder(sortOrder++);
            evidence.setCreateTime(LocalDateTime.now());
            evidence.setUpdateTime(LocalDateTime.now());
        }
        return this.saveBatch(evidences);
    }
}
