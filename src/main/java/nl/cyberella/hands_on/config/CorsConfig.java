/**
 * CORS configuration for the application.
 *
 * CorsConfig is annotated with @Configuration and lives under nl.cyberella.hands_on.config,
 * a subpackage of your main application package (nl.cyberella.hands_on). Spring Boot's component
 * scan will pick it up at startup and register its WebMvcConfigurer bean.
 * When registered, Spring MVC applies the addCorsMappings(...) rules so
 * responses include the appropriate CORS headers for matching requests
 * (including preflight OPTIONS), provided the request reaches the MVC layer.
 */

package nl.cyberella.hands_on.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    /**
     * Register a WebMvcConfigurer to define CORS rules.
     *
     * This allows the browser to make cross-origin requests from the
     * configured frontend origin (for example, the Vite dev server).
     * The mapping uses /** to cover all application endpoints.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Allow the Vite development server origin to call this API.
                // In production, change this to your deployed frontend origin(s).
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173") // the Vite frontend
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        // If your frontend sends credentials (cookies or Authorization
                        // header with credentials mode), keep this as true and list
                        // exact origins above (browsers disallow "*" with credentials).
                        .allowCredentials(true);
            }
        };
    }
}
