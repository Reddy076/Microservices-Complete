package com.revature.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GeneratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeneratorServiceApplication.class, args);
    }
}

