package com.tokenhub.usercenter.infrastructure.mail;

import com.tokenhub.usercenter.application.port.VerificationMailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingVerificationMailAdapter implements VerificationMailPort {

  private static final Logger log = LoggerFactory.getLogger(LoggingVerificationMailAdapter.class);

  @Override
  public void sendLoginOrVerificationHint(String email, String subject, String body) {
    log.info("VerificationMail to={} subject={} body={}", email, subject, body);
  }
}
