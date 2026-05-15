package com.tokenhub.payment.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChannelReconciliationLineMapper extends BaseMapper<ChannelReconciliationLinePo> {}
