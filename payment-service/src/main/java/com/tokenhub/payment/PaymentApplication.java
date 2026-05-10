package com.tokenhub.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = "com.tokenhub")
@MapperScan("com.tokenhub.payment.infrastructure.persistence")
@EnableTransactionManagement
@EnableScheduling
public class PaymentApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentApplication.class, args);
  }

  @Bean
  RestTemplate paymentRestTemplate() {
    return new RestTemplate();
  }
}
