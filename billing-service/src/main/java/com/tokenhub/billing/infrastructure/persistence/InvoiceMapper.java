package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoiceMapper extends BaseMapper<InvoicePo> {}
