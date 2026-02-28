package com.example.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {
    private static final Logger log = LoggerFactory.getLogger(ApiApplication.class);
    public static void main(String[] args) {
        log.info("ApiApplication is starting...");
        SpringApplication.run(ApiApplication.class, args);
        log.info("ApiApplication has started successfully.");
    }

}
