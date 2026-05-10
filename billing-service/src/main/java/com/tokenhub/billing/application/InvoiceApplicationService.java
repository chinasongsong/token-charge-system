package com.tokenhub.billing.application;

import com.tokenhub.billing.infrastructure.persistence.InvoiceMapper;
import com.tokenhub.billing.infrastructure.persistence.InvoicePo;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class InvoiceApplicationService {

  private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

  private final InvoiceMapper invoiceMapper;

  public InvoiceApplicationService(InvoiceMapper invoiceMapper) {
    this.invoiceMapper = invoiceMapper;
  }

  public InvoicePo issuePlaceholder(long userId, String orderRef, long amount, String currency) {
    String pdf = "INV-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
        + "-" + SEQ.incrementAndGet();
    InvoicePo row = new InvoicePo();
    row.setUserId(userId);
    row.setOrderRef(orderRef);
    row.setAmount(amount);
    row.setCurrency(currency != null ? currency : "CNY");
    row.setPdfNumber(pdf);
    row.setStatus("ISSUED");
    invoiceMapper.insert(row);
    return row;
  }
}
