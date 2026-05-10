package com.tokenhub.usercenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tokenhub")
public class UserCenterApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserCenterApplication.class, args);
  }
}
