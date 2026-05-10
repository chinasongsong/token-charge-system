package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.billing.infrastructure.persistence.ApiKeyMapper;
import com.tokenhub.billing.infrastructure.persistence.ApiKeyPo;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.common.security.apikey.ApiKeySupport;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyApplicationService {

  private static final String KEY_PREFIX = "sk_tokenhub_";

  private final ApiKeyMapper apiKeyMapper;
  private final AccountBalanceApplicationService accountBalanceApplicationService;

  public ApiKeyApplicationService(
      ApiKeyMapper apiKeyMapper,
      AccountBalanceApplicationService accountBalanceApplicationService
  ) {
    this.apiKeyMapper = apiKeyMapper;
    this.accountBalanceApplicationService = accountBalanceApplicationService;
  }

  public record CreatedApiKey(long id, String name, String plaintextKey, String status, String createdAt) {}

  @Transactional
  public CreatedApiKey create(long userId, String name) {
    accountBalanceApplicationService.getOrCreate(userId);
    byte[] rnd = new byte[24];
    new SecureRandom().nextBytes(rnd);
    String plaintext = KEY_PREFIX + HexFormat.of().formatHex(rnd);
    String fingerprint = ApiKeySupport.sha256HexUtf8(plaintext);
    ApiKeyPo row = new ApiKeyPo();
    row.setUserId(userId);
    row.setName(name);
    row.setFingerprint(fingerprint);
    row.setStatus("ACTIVE");
    apiKeyMapper.insert(row);
    ApiKeyPo saved = apiKeyMapper.selectById(row.getId());
    return new CreatedApiKey(
        saved.getId(),
        name,
        plaintext,
        saved.getStatus(),
        saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : ""
    );
  }

  public List<ApiKeyPo> listForUser(long userId) {
    return apiKeyMapper.selectList(
        new LambdaQueryWrapper<ApiKeyPo>()
            .eq(ApiKeyPo::getUserId, userId)
            .orderByDesc(ApiKeyPo::getCreatedAt)
    );
  }

  @Transactional
  public void disable(long userId, long apiKeyId) {
    ApiKeyPo row = apiKeyMapper.selectById(apiKeyId);
    if (row == null || !row.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "API Key 不存在");
    }
    row.setStatus("DISABLED");
    apiKeyMapper.updateById(row);
  }

  public ApiKeyPo requireActiveByFingerprint(String fingerprint) {
    ApiKeyPo row = apiKeyMapper.selectOne(
        new LambdaQueryWrapper<ApiKeyPo>().eq(ApiKeyPo::getFingerprint, fingerprint)
    );
    if (row == null || !"ACTIVE".equalsIgnoreCase(row.getStatus())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 API Key");
    }
    return row;
  }

  public Optional<ApiKeyPo> findActiveByFingerprint(String fingerprint) {
    ApiKeyPo row = apiKeyMapper.selectOne(
        new LambdaQueryWrapper<ApiKeyPo>().eq(ApiKeyPo::getFingerprint, fingerprint)
    );
    if (row == null || !"ACTIVE".equalsIgnoreCase(row.getStatus())) {
      return Optional.empty();
    }
    return Optional.of(row);
  }
}
