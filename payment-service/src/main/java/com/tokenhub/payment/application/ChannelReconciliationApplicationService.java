package com.tokenhub.payment.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.payment.infrastructure.persistence.ChannelReconciliationBatchMapper;
import com.tokenhub.payment.infrastructure.persistence.ChannelReconciliationBatchPo;
import com.tokenhub.payment.infrastructure.persistence.ChannelReconciliationLineMapper;
import com.tokenhub.payment.infrastructure.persistence.ChannelReconciliationLinePo;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderMapper;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderPo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O-4 渠道对账（M1：CSV 导入与平台内比对）。
 *
 * <p>CSV 行格式（首行可选 header，包含「channel_order_no」时跳过）：
 * <pre>channel_order_no,local_order_no,channel_amount,channel_status,paid_at(YYYY-MM-DDTHH:mm:ss)</pre>
 *
 * <p>diff_kind：
 * <ul>
 *   <li>{@code MATCHED} 渠道与本地金额一致且本地 PAID</li>
 *   <li>{@code AMOUNT_MISMATCH} 金额不一致</li>
 *   <li>{@code MISSING_LOCAL} 渠道有、本地查无</li>
 *   <li>{@code LOCAL_INIT} 渠道 SUCCESS 但本地仍 INIT（建议触发 retry-credit）</li>
 *   <li>{@code LOCAL_OTHER} 其它本地终态（REFUND/CLOSED 等）</li>
 * </ul>
 */
@Service
public class ChannelReconciliationApplicationService {

  private static final DateTimeFormatter PAID_AT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final ChannelReconciliationBatchMapper batchMapper;
  private final ChannelReconciliationLineMapper lineMapper;
  private final PaymentOrderMapper paymentOrderMapper;

  public ChannelReconciliationApplicationService(
      ChannelReconciliationBatchMapper batchMapper,
      ChannelReconciliationLineMapper lineMapper,
      PaymentOrderMapper paymentOrderMapper
  ) {
    this.batchMapper = batchMapper;
    this.lineMapper = lineMapper;
    this.paymentOrderMapper = paymentOrderMapper;
  }

  public record ImportResult(long batchId, int total, int matched, int mismatched) {}

  @Transactional
  public ImportResult importBatch(String channel, LocalDate billDate, String sourceName, String csv) {
    if (channel == null || channel.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "channel 不能为空");
    }
    if (billDate == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "billDate 不能为空");
    }
    if (sourceName == null || sourceName.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "sourceName 不能为空");
    }
    if (csv == null || csv.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "对账数据为空");
    }

    ChannelReconciliationBatchPo batch = new ChannelReconciliationBatchPo();
    batch.setChannel(channel);
    batch.setBillDate(billDate);
    batch.setSourceName(sourceName);
    batch.setTotalLines(0);
    batch.setMatchedLines(0);
    batch.setMismatchedLines(0);
    batch.setStatus("IMPORTED");
    batchMapper.insert(batch);

    int matched = 0;
    int mismatched = 0;
    int total = 0;
    List<ChannelReconciliationLinePo> lines = new ArrayList<>();
    for (String raw : csv.split("\\r?\\n")) {
      String line = raw == null ? "" : raw.trim();
      if (line.isEmpty()) {
        continue;
      }
      if (total == 0 && line.toLowerCase().contains("channel_order_no")) {
        continue;
      }
      total++;
      String[] cols = line.split(",", -1);
      if (cols.length < 4) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "对账行格式错误：" + line);
      }
      String channelOrderNo = cols[0].trim();
      String localOrderNoHint = cols[1].trim();
      long channelAmount;
      try {
        channelAmount = Long.parseLong(cols[2].trim());
      } catch (NumberFormatException ex) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "金额非法：" + cols[2]);
      }
      String channelStatus = cols[3].trim();
      LocalDateTime paidAt = null;
      if (cols.length >= 5 && !cols[4].isBlank()) {
        try {
          paidAt = LocalDateTime.parse(cols[4].trim(), PAID_AT_FORMAT);
        } catch (Exception ignored) {
          paidAt = null;
        }
      }

      PaymentOrderPo local = lookupLocalOrder(channelOrderNo, localOrderNoHint);
      ChannelReconciliationLinePo lineRow = new ChannelReconciliationLinePo();
      lineRow.setBatchId(batch.getId());
      lineRow.setChannelOrderNo(channelOrderNo);
      lineRow.setChannelAmount(channelAmount);
      lineRow.setChannelStatus(channelStatus);
      lineRow.setPaidAt(paidAt);
      String diff;
      if (local == null) {
        diff = "MISSING_LOCAL";
      } else {
        lineRow.setLocalOrderNo(local.getOrderNo());
        lineRow.setUserId(local.getUserId());
        lineRow.setLocalAmount(local.getAmount());
        lineRow.setLocalStatus(local.getStatus());
        if (!local.getAmount().equals(channelAmount)) {
          diff = "AMOUNT_MISMATCH";
        } else if ("INIT".equalsIgnoreCase(local.getStatus())) {
          diff = "LOCAL_INIT";
        } else if ("PAID".equalsIgnoreCase(local.getStatus())) {
          diff = "MATCHED";
        } else {
          diff = "LOCAL_OTHER";
        }
      }
      lineRow.setDiffKind(diff);
      if ("MATCHED".equals(diff)) {
        matched++;
      } else {
        mismatched++;
      }
      lines.add(lineRow);
    }
    for (ChannelReconciliationLinePo lineRow : lines) {
      lineMapper.insert(lineRow);
    }
    batch.setTotalLines(total);
    batch.setMatchedLines(matched);
    batch.setMismatchedLines(mismatched);
    batch.setStatus("RECONCILED");
    batchMapper.updateById(batch);
    return new ImportResult(batch.getId(), total, matched, mismatched);
  }

  public record BatchSummary(
      long batchId,
      String channel,
      LocalDate billDate,
      String sourceName,
      int total,
      int matched,
      int mismatched,
      List<ChannelReconciliationLinePo> mismatchedSample
  ) {}

  public BatchSummary getBatchSummary(long batchId) {
    ChannelReconciliationBatchPo batch = batchMapper.selectById(batchId);
    if (batch == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "对账批次不存在");
    }
    List<ChannelReconciliationLinePo> sample = lineMapper.selectList(
        new LambdaQueryWrapper<ChannelReconciliationLinePo>()
            .eq(ChannelReconciliationLinePo::getBatchId, batchId)
            .ne(ChannelReconciliationLinePo::getDiffKind, "MATCHED")
            .orderByAsc(ChannelReconciliationLinePo::getId)
            .last("LIMIT 100")
    );
    return new BatchSummary(
        batch.getId(),
        batch.getChannel(),
        batch.getBillDate(),
        batch.getSourceName(),
        batch.getTotalLines(),
        batch.getMatchedLines(),
        batch.getMismatchedLines(),
        sample
    );
  }

  private PaymentOrderPo lookupLocalOrder(String channelOrderNo, String localOrderNoHint) {
    if (localOrderNoHint != null && !localOrderNoHint.isBlank()) {
      PaymentOrderPo p =
          paymentOrderMapper.selectOne(
              new LambdaQueryWrapper<PaymentOrderPo>().eq(PaymentOrderPo::getOrderNo, localOrderNoHint)
          );
      if (p != null) {
        return p;
      }
    }
    return paymentOrderMapper.selectOne(
        new LambdaQueryWrapper<PaymentOrderPo>().eq(PaymentOrderPo::getOrderNo, channelOrderNo)
    );
  }
}
