package com.tokenhub.usercenter.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.usercenter.application.dto.SupportTicketItem;
import com.tokenhub.usercenter.application.dto.SupportTicketMessageItem;
import com.tokenhub.usercenter.infrastructure.persistence.SupportTicketMapper;
import com.tokenhub.usercenter.infrastructure.persistence.SupportTicketMessageMapper;
import com.tokenhub.usercenter.infrastructure.persistence.SupportTicketMessagePo;
import com.tokenhub.usercenter.infrastructure.persistence.SupportTicketPo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketApplicationService {

  private final SupportTicketMapper supportTicketMapper;
  private final SupportTicketMessageMapper supportTicketMessageMapper;

  public SupportTicketApplicationService(
      SupportTicketMapper supportTicketMapper,
      SupportTicketMessageMapper supportTicketMessageMapper
  ) {
    this.supportTicketMapper = supportTicketMapper;
    this.supportTicketMessageMapper = supportTicketMessageMapper;
  }

  public List<SupportTicketItem> listForUser(long userId) {
    List<SupportTicketPo> rows = supportTicketMapper.selectList(
        new LambdaQueryWrapper<SupportTicketPo>()
            .eq(SupportTicketPo::getUserId, userId)
            .orderByDesc(SupportTicketPo::getUpdatedAt)
    );
    return rows.stream().map(SupportTicketApplicationService::toItem).toList();
  }

  public SupportTicketItem create(long userId, String title) {
    SupportTicketPo row = new SupportTicketPo();
    row.setUserId(userId);
    row.setTitle(title);
    row.setStatus("OPEN");
    row.setPriority("NORMAL");
    supportTicketMapper.insert(row);
    return toItem(row);
  }

  public List<SupportTicketMessageItem> listMessages(long userId, long ticketId) {
    assertTicketOwned(userId, ticketId);
    List<SupportTicketMessagePo> rows =
        supportTicketMessageMapper.selectList(
            new LambdaQueryWrapper<SupportTicketMessagePo>()
                .eq(SupportTicketMessagePo::getTicketId, ticketId)
                .orderByAsc(SupportTicketMessagePo::getCreatedAt)
        );
    return rows.stream().map(SupportTicketApplicationService::toMessageItem).toList();
  }

  @Transactional
  public SupportTicketMessageItem appendUserMessage(long userId, long ticketId, String body) {
    assertTicketOwned(userId, ticketId);
    SupportTicketMessagePo row = new SupportTicketMessagePo();
    row.setTicketId(ticketId);
    row.setUserId(userId);
    row.setRole("USER");
    row.setBody(body);
    supportTicketMessageMapper.insert(row);
    SupportTicketMessagePo loaded = supportTicketMessageMapper.selectById(row.getId());
    String preview = body.trim();
    if (preview.length() > 500) {
      preview = preview.substring(0, 500);
    }
    supportTicketMapper.update(
        null,
        new LambdaUpdateWrapper<SupportTicketPo>()
            .eq(SupportTicketPo::getId, ticketId)
            .set(SupportTicketPo::getLastMessagePreview, preview)
    );
    return toMessageItem(loaded != null ? loaded : row);
  }

  private void assertTicketOwned(long userId, long ticketId) {
    SupportTicketPo t = supportTicketMapper.selectById(ticketId);
    if (t == null || !t.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
    }
  }

  private static SupportTicketMessageItem toMessageItem(SupportTicketMessagePo row) {
    return new SupportTicketMessageItem(
        row.getId(),
        row.getTicketId(),
        row.getUserId(),
        row.getRole(),
        row.getBody(),
        row.getCreatedAt()
    );
  }

  private static SupportTicketItem toItem(SupportTicketPo row) {
    return new SupportTicketItem(
        row.getId(),
        row.getTitle(),
        row.getStatus(),
        row.getPriority(),
        row.getCreatedAt(),
        row.getUpdatedAt()
    );
  }
}
