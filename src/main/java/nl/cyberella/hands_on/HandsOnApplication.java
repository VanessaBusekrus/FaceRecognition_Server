// This is the main application class for the HandsOn Spring Boot application -> the entry point.
// It also contains a security configuration bean that disables security for all endpoints.

package nl.cyberella.hands_on;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// SpringBootApplication annotation indicates this is a Spring Boot application-
// It includes component scanning (to scan packages), auto-configuration (to configure beans), and property support (to load application properties).
@SpringBootApplication
public class HandsOnApplication {

    // This is the main method that serves as the entry point for the Spring Boot application.
    public static void main(String[] args) {
        SpringApplication.run(HandsOnApplication.class, args);
    }

    // Bean to configure security for all endpoints.
    // A bean is required to override the default security configuration.
    // Spring will call this method and use the returned SecurityFilterChain to configure security in the application.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is now ENABLED (default behavior)
                // For stateless REST APIs with token-based auth, you might want:
                // .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                // Or use CSRF tokens in your frontend
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // Tells Spring Security to allow all requests without authentication.
        return http.build(); // Builds the SecurityFilterChain object from the configured HttpSecurity and returns it to Spring to register as the active web security chain.
    }
}
