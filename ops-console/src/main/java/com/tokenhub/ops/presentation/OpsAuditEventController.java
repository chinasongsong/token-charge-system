package com.tokenhub.ops.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/audit-events")
public class OpsAuditEventController {

  private final JdbcTemplate jdbcTemplate;

  public OpsAuditEventController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record AuditEventRow(
      long id,
      String actor,
      String action,
      String resourceType,
      String resourceId,
      String detailJson,
      Instant createdAt
  ) {}

  @GetMapping
  public ApiResponse<List<AuditEventRow>> list(
      @RequestParam(name = "limit", defaultValue = "100") int limit
  ) {
    int safe = Math.min(Math.max(limit, 1), 500);
    List<AuditEventRow> rows =
        jdbcTemplate.query(
            "SELECT id, actor, action, resource_type, resource_id, detail_json, created_at "
                + "FROM audit_events ORDER BY id DESC LIMIT ?",
            ps -> ps.setInt(1, safe),
            (rs, i) ->
                new AuditEventRow(
                    rs.getLong("id"),
                    rs.getString("actor"),
                    rs.getString("action"),
                    rs.getString("resource_type"),
                    rs.getString("resource_id"),
                    rs.getString("detail_json"),
                    toInstant(rs.getTimestamp("created_at"))
                )
        );
    return ApiResponse.ok(rows);
  }

  private static Instant toInstant(Timestamp ts) {
    return ts == null ? null : ts.toInstant();
  }
}
