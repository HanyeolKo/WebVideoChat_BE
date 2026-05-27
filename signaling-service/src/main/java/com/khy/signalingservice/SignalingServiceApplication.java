package com.khy.signalingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SignalingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SignalingServiceApplication.class, args);
    }
}
