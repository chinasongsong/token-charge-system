package com.tokenhub.ops.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/model-providers")
public class OpsModelProviderController {

  private final JdbcTemplate jdbcTemplate;

  public OpsModelProviderController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record ModelProviderRow(String code, String title, boolean enabled, String baseUrl) {}

  @GetMapping
  public ApiResponse<List<ModelProviderRow>> list() {
    List<ModelProviderRow> rows = jdbcTemplate.query(
        "SELECT code, title, enabled, base_url FROM model_providers ORDER BY code",
        (rs, i) -> new ModelProviderRow(
            rs.getString("code"),
            rs.getString("title"),
            rs.getBoolean("enabled"),
            rs.getString("base_url")
        )
    );
    return ApiResponse.ok(rows);
  }
}
