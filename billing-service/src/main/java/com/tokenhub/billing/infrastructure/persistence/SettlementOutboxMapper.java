package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SettlementOutboxMapper extends BaseMapper<SettlementOutboxPo> {

  /**
   * 拉取一批待发布事件（按 id 升序），仅限 PENDING 且未到 next_attempt_at 或 next_attempt_at 为 NULL。
   *
   * <p>开发期使用普通 SELECT；生产可换为 {@code FOR UPDATE SKIP LOCKED}（MySQL 8）避免多实例同时拉同一行。
   */
  @Select(
      "SELECT * FROM settlement_outbox "
          + "WHERE status = 'PENDING' "
          + "AND (next_attempt_at IS NULL OR next_attempt_at <= NOW(3)) "
          + "ORDER BY id ASC LIMIT #{limit}"
  )
  List<SettlementOutboxPo> claimPendingBatch(@Param("limit") int limit);
}
