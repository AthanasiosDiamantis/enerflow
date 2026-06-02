package de.saki.enerflow.config;

import de.saki.enerflow.adapter.heatpump.novelan.NovelanHeatpumpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Spring configuration for the Novelan heat pump WebSocket client.
 * Creates and connects the client on application startup.
 *
 * @author saki
 */
@Configuration
public class HeatpumpConfig {

    private static final Logger log = LoggerFactory.getLogger(HeatpumpConfig.class);

    private final HeatpumpProperties properties;

    public HeatpumpConfig(HeatpumpProperties properties) {
        this.properties = properties;
    }

    @Bean
    public NovelanHeatpumpClient novelanHeatpumpClient() {
        URI uri = URI.create(
                "ws://" + properties.getHost() + ":" + properties.getPort()
        );

        log.info("Connecting to heat pump at {}", uri);

        NovelanHeatpumpClient client = new NovelanHeatpumpClient(uri, properties.getPassword());

        try {
            client.connectBlocking();
            log.info("Heat pump connection established, waiting for login...");

            // FIXME: replace with event-driven approach in production
            // Pump needs ~2s to process login before accepting REFRESH
            Thread.sleep(2000);

            log.info("Sending REFRESH...");
            client.send("REFRESH");

        } catch (InterruptedException e) {
            log.error("Connection interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        return client;
    }

}
