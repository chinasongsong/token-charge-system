package com.tokenhub.usercenter.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.usercenter.application.SupportTicketApplicationService;
import com.tokenhub.usercenter.domain.auth.AuthConstants;
import com.tokenhub.usercenter.application.dto.SupportTicketItem;
import com.tokenhub.usercenter.application.dto.SupportTicketMessageItem;
import com.tokenhub.usercenter.presentation.dto.SupportTicketCreateRequest;
import com.tokenhub.usercenter.presentation.dto.SupportTicketReplyRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/support/tickets")
@Validated
public class SupportTicketController {

  private final SupportTicketApplicationService supportTicketApplicationService;

  public SupportTicketController(SupportTicketApplicationService supportTicketApplicationService) {
    this.supportTicketApplicationService = supportTicketApplicationService;
  }

  @GetMapping
  public ApiResponse<List<SupportTicketItem>> list(HttpServletRequest http) {
    long userId = (Long) http.getAttribute(AuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(supportTicketApplicationService.listForUser(userId));
  }

  @PostMapping
  public ApiResponse<SupportTicketItem> create(
      @Valid @RequestBody SupportTicketCreateRequest request,
      HttpServletRequest http
  ) {
    long userId = (Long) http.getAttribute(AuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(supportTicketApplicationService.create(userId, request.title().trim()));
  }

  @GetMapping("/{ticketId}/messages")
  public ApiResponse<List<SupportTicketMessageItem>> listMessages(
      @PathVariable long ticketId,
      HttpServletRequest http
  ) {
    long userId = (Long) http.getAttribute(AuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(supportTicketApplicationService.listMessages(userId, ticketId));
  }

  @PostMapping("/{ticketId}/messages")
  public ApiResponse<SupportTicketMessageItem> reply(
      @PathVariable long ticketId,
      @Valid @RequestBody SupportTicketReplyRequest request,
      HttpServletRequest http
  ) {
    long userId = (Long) http.getAttribute(AuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(
        supportTicketApplicationService.appendUserMessage(userId, ticketId, request.body().trim())
    );
  }
}
