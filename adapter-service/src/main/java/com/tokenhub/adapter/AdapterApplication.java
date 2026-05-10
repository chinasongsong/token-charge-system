package com.tokenhub.adapter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tokenhub")
@MapperScan("com.tokenhub.adapter.infrastructure.persistence")
public class AdapterApplication {

  public static void main(String[] args) {
    SpringApplication.run(AdapterApplication.class, args);
  }
}
