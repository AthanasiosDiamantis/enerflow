package de.saki.enerflow.scheduler;

import de.saki.enerflow.core.service.EnergyManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers the EnerFlow PV-surplus decision logic.
 * Runs every {@code enerflow.evaluation-interval-ms} milliseconds (default 60s).
 *
 * @author saki
 */
@Component
@Profile("!test")
public class EnergyManagerScheduler {

    private static final Logger log = LoggerFactory.getLogger(EnergyManagerScheduler.class);

    private final EnergyManagerService energyManagerService;

    public EnergyManagerScheduler(EnergyManagerService energyManagerService) {
        this.energyManagerService = energyManagerService;
    }

    @Scheduled(fixedRateString = "${enerflow.evaluation-interval-ms}")
    public void evaluate() {
        log.debug("EnergyManagerScheduler triggered - evaluating PV-surplus conditions...");
        try{
            energyManagerService.evaluateAndAct();
        } catch (Exception e) {
            // Catch-all to ensure scheduler keeps running even if a single
            // evaluation cycle fails (e.g. temporary hardware unavailability)
            log.error("Error during EnergyManager evaluation: {}", e.getMessage(), e);
        }
    }
}
