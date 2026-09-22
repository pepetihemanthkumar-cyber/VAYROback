package com.vayro.booking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Safe startup diagnostics logger for production verification.
 * Adheres strictly to security standards: NEVER logs passwords, JWT secrets, or full credentialed URIs.
 */
@Component
public class DatabaseStartupDiagnostics {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupDiagnostics.class);

    private final Environment environment;

    @Value("${server.port:8083}")
    private String serverPort;

    @Value("${DB_HOST:aws-0-ap-southeast-1.pooler.supabase.com}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:postgres}")
    private String dbName;

    @Value("${DB_USERNAME:postgres.lgpuulmsdogxzsyowqdk}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    public DatabaseStartupDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDiagnostics() {
        log.info("==================================================================");
        log.info("             BOOKING SERVICE STARTUP DIAGNOSTICS                  ");
        log.info("==================================================================");
        log.info("Active Profiles       : {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("Effective Server Port : {}", serverPort);
        log.info("Database Host         : {}", dbHost);
        log.info("Database Port         : {}", dbPort);
        log.info("Database Name         : {}", dbName);
        log.info("Database Username     : {}", dbUsername);
        log.info("DB Password Configured: {}", (dbPassword != null && !dbPassword.isBlank()));
        log.info("Target JDBC Endpoint  : jdbc:postgresql://{}:{}/{}?sslmode=require", dbHost, dbPort, dbName);
        log.info("==================================================================");
    }
}
