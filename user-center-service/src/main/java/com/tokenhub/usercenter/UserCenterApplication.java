package com.tokenhub.usercenter;

import com.tokenhub.usercenter.infrastructure.security.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.tokenhub")
@MapperScan("com.tokenhub.usercenter.infrastructure.persistence")
@EnableConfigurationProperties(JwtProperties.class)
public class UserCenterApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserCenterApplication.class, args);
  }
}
