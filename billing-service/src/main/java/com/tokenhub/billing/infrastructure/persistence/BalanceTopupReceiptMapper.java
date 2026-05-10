package com.tokenhub.billing.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BalanceTopupReceiptMapper {

  /** @return 插入行数：1 表示首次，0 表示幂等重复 */
  @Insert("""
      INSERT IGNORE INTO balance_topup_receipts (source_ref, user_id, amount)
      VALUES (#{sourceRef}, #{userId}, #{amount})
      """)
  int insertIgnore(
      @Param("sourceRef") String sourceRef,
      @Param("userId") long userId,
      @Param("amount") long amount
  );
}
