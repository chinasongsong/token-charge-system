package com.tokenhub.common.core.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.tokenhub.common.core.error.ErrorCode;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

  @Test
  void okCarriesBusinessPayload() {
    ApiResponse<String> body = ApiResponse.ok("demo");
    assertThat(body.code()).isEqualTo(ErrorCode.SUCCESS.getCode());
    assertThat(body.data()).isEqualTo("demo");
  }

  @Test
  void failSetsCode() {
    ApiResponse<Void> body = ApiResponse.fail(ErrorCode.NOT_FOUND.getCode(), "gone");
    assertThat(body.code()).isEqualTo(ErrorCode.NOT_FOUND.getCode());
    assertThat(body.message()).isEqualTo("gone");
    assertThat(body.data()).isNull();
  }
}
