package de.saki.enerflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Novelan heat pump connection.
 * Values are located in application.yaml under the "heatpump" prefix.
 *
 * @author saki
 */
@Component
@ConfigurationProperties(prefix = "heatpump")
public class HeatpumpProperties {

    private String host;
    private int port;
    private String password;
    private int reconnectDelaySeconds;

    // Getters and setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getReconnectDelaySeconds() {
        return reconnectDelaySeconds;
    }

    public void setReconnectDelaySeconds(int reconnectDelaySeconds) {
        this.reconnectDelaySeconds = reconnectDelaySeconds;
    }
}
