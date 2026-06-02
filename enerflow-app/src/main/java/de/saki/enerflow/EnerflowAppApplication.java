package de.saki.enerflow;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan // Scans the
public class EnerflowAppApplication {

    public static void main(String[] args) {

        // Load .env file and inject into system properties
        // so Spring Boot can resolve ${HEATPUMP_PASSWORD} in application.yaml
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(EnerflowAppApplication.class, args);
    }

}
