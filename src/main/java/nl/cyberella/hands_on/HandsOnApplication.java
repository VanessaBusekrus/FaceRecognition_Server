// This is the main application class for the HandsOn Spring Boot application -> the entry point.

package nl.cyberella.hands_on;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// SpringBootApplication annotation indicates this is a Spring Boot application-
// It includes component scanning (to scan packages), auto-configuration (to configure beans), and property support (to load application properties).
@SpringBootApplication
public class HandsOnApplication {

    // This is the main method that serves as the entry point for the Spring Boot application.
    public static void main(String[] args) {
        SpringApplication.run(HandsOnApplication.class, args);
    }
}
