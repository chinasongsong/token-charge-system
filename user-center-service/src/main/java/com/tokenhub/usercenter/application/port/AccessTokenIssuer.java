package com.tokenhub.usercenter.application.port;

public interface AccessTokenIssuer {

  String issueForUser(long userId);

  long accessTokenTtlSeconds();
}
