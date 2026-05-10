package com.tokenhub.common.security.apikey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ApiKeySupportTest {

  @Test
  void bearerExtractionWorks() {
    assertEquals("abc", ApiKeySupport.extractBearer("Bearer abc"));
    assertTrue(ApiKeySupport.looksLikeBearer("bearer xyz"));
    assertFalse(ApiKeySupport.looksLikeBearer("ApiKey xxx"));
  }
}
