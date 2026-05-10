package com.tokenhub.usercenter.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.common.security.apikey.ApiKeySupport;
import com.tokenhub.usercenter.domain.auth.PasswordResetRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetRepositoryImpl implements PasswordResetRepository {

  private final PasswordResetCodeMapper mapper;

  public PasswordResetRepositoryImpl(PasswordResetCodeMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void savePendingCode(String email, String codeHash, Instant expiresAt) {
    PasswordResetCodePo po = new PasswordResetCodePo();
    po.setEmail(email);
    po.setCodeHash(codeHash);
    po.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
    po.setConsumed(Boolean.FALSE);
    mapper.insert(po);
  }

  @Override
  public boolean tryConsumeLatest(String email, String plainCode, Instant now) {
    String hash = ApiKeySupport.sha256HexUtf8(plainCode);
    LocalDateTime t = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
    PasswordResetCodePo row = mapper.selectOne(
        new LambdaQueryWrapper<PasswordResetCodePo>()
            .eq(PasswordResetCodePo::getEmail, email)
            .eq(PasswordResetCodePo::getConsumed, Boolean.FALSE)
            .eq(PasswordResetCodePo::getCodeHash, hash)
            .gt(PasswordResetCodePo::getExpiresAt, t)
            .orderByDesc(PasswordResetCodePo::getId)
            .last("LIMIT 1")
    );
    if (row == null) {
      return false;
    }
    row.setConsumed(Boolean.TRUE);
    mapper.updateById(row);
    return true;
  }
}
