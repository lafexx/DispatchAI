package com.dispatchai.fleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class FleetServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FleetServiceApplication.class, args);
  }

}
