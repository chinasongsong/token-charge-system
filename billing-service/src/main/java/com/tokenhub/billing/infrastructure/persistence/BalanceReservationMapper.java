package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BalanceReservationMapper extends BaseMapper<BalanceReservationPo> {

  /** 计算用户当前仍有效的预占额度合计（RESERVED 且未过期）。 */
  @Select(
      "SELECT COALESCE(SUM(amount), 0) FROM balance_reservations "
          + "WHERE user_id = #{userId} AND status = 'RESERVED' AND expires_at > #{now}"
  )
  long sumActiveReservedAmount(@Param("userId") long userId, @Param("now") LocalDateTime now);

  /** 把过期但仍为 RESERVED 的行批量标记为 EXPIRED（清理任务用）。 */
  @Update(
      "UPDATE balance_reservations SET status = 'EXPIRED' "
          + "WHERE status = 'RESERVED' AND expires_at <= #{now}"
  )
  int markExpired(@Param("now") LocalDateTime now);
}
