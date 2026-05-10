package com.tokenhub.payment.application;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.payment.application.dto.MockCallbackRequest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockCallbackSignatureVerifier {

  private final byte[] secret;

  public MockCallbackSignatureVerifier(
      @Value("${MOCK_CALLBACK_SECRET:dev-mock-callback-secret-change}") String secretRaw
  ) {
    this.secret = secretRaw.getBytes(StandardCharsets.UTF_8);
  }

  public void verify(MockCallbackRequest req) {
    if (!"PAID".equalsIgnoreCase(req.status().trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 status=PAID 的入账回调");
    }
    TreeMap<String, String> m = new TreeMap<>();
    m.put("amount", String.valueOf(req.amount()));
    m.put("orderNo", req.orderNo());
    m.put("status", req.status().trim());
    m.put("ts", String.valueOf(req.ts()));
    m.put("userId", String.valueOf(req.userId()));
    StringBuilder canonical = new StringBuilder();
    for (var e : m.entrySet()) {
      if (canonical.length() > 0) {
        canonical.append('&');
      }
      canonical.append(e.getKey()).append('=').append(e.getValue());
    }
    String expectHex;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw = mac.doFinal(canonical.toString().getBytes(StandardCharsets.UTF_8));
      expectHex = HexFormat.of().formatHex(raw);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL, "签名校验失败");
    }
    if (!constantTimeEquals(expectHex, req.signature().trim())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "回调签名无效");
    }
    if (Math.abs(System.currentTimeMillis() - req.ts() * 1000L) > 15 * 60_000L) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "回调时间戳过期");
    }
  }

  private static boolean constantTimeEquals(String a, String b) {
    byte[] x = a.getBytes(StandardCharsets.UTF_8);
    byte[] y = b.getBytes(StandardCharsets.UTF_8);
    if (x.length != y.length) {
      return false;
    }
    int r = 0;
    for (int i = 0; i < x.length; i++) {
      r |= x[i] ^ y[i];
    }
    return r == 0;
  }
}
