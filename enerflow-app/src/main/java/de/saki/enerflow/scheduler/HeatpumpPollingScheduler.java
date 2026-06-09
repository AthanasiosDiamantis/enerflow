package de.saki.enerflow.scheduler;

import de.saki.enerflow.adapter.heatpump.novelan.NovelanHeatpumpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that triggers periodic heat pump data polling.
 * Sends REFRESH every 60 seconds to receive updated sensor data.
 *
 * @author saki
 */
@Component
public class HeatpumpPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(HeatpumpPollingScheduler.class);

    private final NovelanHeatpumpClient heatpumpClient;

    public HeatpumpPollingScheduler(NovelanHeatpumpClient heatpumpClient) {
        this.heatpumpClient = heatpumpClient;
    }

    /**
     * Polls the heat pump every 60 seconds.
     * Reconnects automatically if the connection was lost.
     */
    @Scheduled(fixedDelay = 60000)
    public void poll() {
        log.debug("Scheduler triggered - checking heat pump connection...");

        if(!heatpumpClient.isOpen()) {
            log.warn("Heat pump connection lost - attempting reconnect...");
            reconnect();
            return;
        }

        log.info("Sending REFRESH to heat pump...");
        heatpumpClient.send("REFRESH");
    }
    private void reconnect() {
        try {
            heatpumpClient.reconnectBlocking();
            log.info("Reconnect successful - waiting for login...");
            // TODO: replace with event-driven approach
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error("Reconnect interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


}
