package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RequestOrderMapper extends BaseMapper<RequestOrderPo> {

  /**
   * 已完成结算但缺少对应 USAGE 流水（异常短款，应人工排查）。
   */
  @Select(
      "SELECT COUNT(*) FROM request_orders ro WHERE ro.billing_status = 'COMPLETED' "
          + "AND NOT EXISTS (SELECT 1 FROM usage_ledger ul WHERE ul.request_order_id = ro.id AND ul.entry_type = 'USAGE')"
  )
  long countCompletedMissingUsageLedger();

  /**
   * 长时间停留在 PENDING 的订单（事务中断或异常路径，应人工/补偿处理）。
   */
  @Select("SELECT COUNT(*) FROM request_orders WHERE billing_status = 'PENDING' AND created_at < #{before}")
  long countStalePending(@Param("before") LocalDateTime before);
}
