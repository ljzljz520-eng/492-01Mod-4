package com.scaffolding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffolding.entity.PaymentBatch;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentBatchMapper extends BaseMapper<PaymentBatch> {
}
