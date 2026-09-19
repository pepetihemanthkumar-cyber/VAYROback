package com.vayro.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * VAYRO Service Registry - Netflix Eureka Server.
 * Acts as the centralized discovery server for all downstream VAYRO microservices:
 * - API-GATEWAY (8080)
 * - USER-SERVICE (8081)
 * - VEHICLE-SERVICE (8082)
 * - BOOKING-SERVICE (8083)
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
