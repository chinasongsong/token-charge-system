package com.tokenhub.usercenter.domain.user;

public interface UserDeviceRepository {

  void recordLogin(long userId, String fingerprint, String userAgent, String ipAddress);
}
