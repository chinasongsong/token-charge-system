package com.tokenhub.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tokenhub")
public class AdapterApplication {

  public static void main(String[] args) {
    SpringApplication.run(AdapterApplication.class, args);
  }
}
