package com.tokenhub.billing.infrastructure.persistence;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * O-7：API Key 生命周期专用 SQL（批量过期 / 触摸 last_used_at）。
 *
 * <p>未直接放在 {@link ApiKeyMapper} 是为了让 BaseMapper CRUD 与生命周期治理 SQL 解耦，便于单测/审计。
 */
@Mapper
public interface ApiKeyLifecycleMapper {

  /** 把已到期但仍为 ACTIVE 的 Key 批量翻转为 EXPIRED。 */
  @Update(
      "UPDATE api_keys SET status = 'EXPIRED' "
          + "WHERE status = 'ACTIVE' AND expires_at IS NOT NULL AND expires_at <= #{now}"
  )
  int markExpired(@Param("now") LocalDateTime now);

  /** 更新 last_used_at（不强一致，best-effort）。 */
  @Update("UPDATE api_keys SET last_used_at = #{now} WHERE id = #{id}")
  int touchLastUsed(@Param("id") long id, @Param("now") LocalDateTime now);
}
