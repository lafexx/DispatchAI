package com.dispatchai.dispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class DispatchServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DispatchServiceApplication.class, args);
  }

}
