package com.mint.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MintSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(MintSearchApplication.class, args);
    }
}
