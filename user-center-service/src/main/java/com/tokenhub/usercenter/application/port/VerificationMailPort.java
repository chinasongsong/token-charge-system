package com.tokenhub.usercenter.application.port;

/**
 * Outbound email / verification channel. P1 logs only; later swap for SMTP or vendor API.
 */
public interface VerificationMailPort {

  void sendLoginOrVerificationHint(String email, String subject, String body);
}
