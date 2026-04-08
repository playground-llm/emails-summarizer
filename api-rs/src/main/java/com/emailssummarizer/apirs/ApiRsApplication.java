package com.emailssummarizer.apirs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the emails-summarizer API resource server ({@code api-rs}).
 *
 * <p>Bootstraps the Spring Boot application context. All configuration is handled
 * through {@code application.yml} and environment variables; no programmatic setup
 * is required here beyond the standard Spring Boot bootstrap.
 */
@SpringBootApplication
public class ApiRsApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args  command-line arguments forwarded to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiRsApplication.class, args);
    }
}
