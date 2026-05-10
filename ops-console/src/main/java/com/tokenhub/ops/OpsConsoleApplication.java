package com.tokenhub.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tokenhub")
public class OpsConsoleApplication {

  public static void main(String[] args) {
    SpringApplication.run(OpsConsoleApplication.class, args);
  }
}
