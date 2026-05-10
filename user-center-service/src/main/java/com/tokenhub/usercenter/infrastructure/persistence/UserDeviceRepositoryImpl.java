package com.tokenhub.usercenter.infrastructure.persistence;

import com.tokenhub.usercenter.domain.user.UserDeviceRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class UserDeviceRepositoryImpl implements UserDeviceRepository {

  private final UserDeviceMapper mapper;

  public UserDeviceRepositoryImpl(UserDeviceMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void recordLogin(long userId, String fingerprint, String userAgent, String ipAddress) {
    UserDevicePo po = new UserDevicePo();
    po.setUserId(userId);
    po.setFingerprint(fingerprint);
    po.setUserAgent(userAgent);
    po.setIpAddress(ipAddress);
    po.setLastLoginAt(LocalDateTime.now());
    mapper.insert(po);
  }
}
