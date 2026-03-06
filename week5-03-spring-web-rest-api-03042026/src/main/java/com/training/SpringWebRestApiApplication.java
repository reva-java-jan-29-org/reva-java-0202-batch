package com.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for the Spring Web REST API demo.
 *
 * @SpringBootApplication combines:
 *   - @Configuration        — marks this class as a source of Spring beans
 *   - @EnableAutoConfiguration — turns on Spring Boot's auto-configuration magic
 *   - @ComponentScan        — scans com.training and all sub-packages for beans
 *
 * When you run this class, Spring Boot:
 *   1. Starts an embedded Tomcat server on port 8080
 *   2. Registers all @RestController, @Service, @Repository beans
 *   3. Sets up Jackson for JSON serialization/deserialization
 *   4. Connects to MySQL using application.properties settings
 */
@SpringBootApplication
public class SpringWebRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringWebRestApiApplication.class, args);
    }
}
