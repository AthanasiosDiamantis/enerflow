package de.saki.enerflow.api.config;

import de.saki.enerflow.core.domain.ElectricityPriceChangeLog;
import de.saki.enerflow.core.domain.ElectricityPriceConfig;
import de.saki.enerflow.core.repository.ElectricityPriceChangeLogRepository;
import de.saki.enerflow.core.repository.ElectricityPriceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for reading and updating the electricity price (US-01-04).
 * Every price change is recorded in electricity_price_change_log.
 *
 * @author saki
 */
@RestController
@RequestMapping("/api/config")
public class ElectricityPriceController {

    private final ElectricityPriceRepository repository;
    private final ElectricityPriceChangeLogRepository changeLogRepository;

    public ElectricityPriceController(
            ElectricityPriceRepository repository,
            ElectricityPriceChangeLogRepository changeLogRepository) {
        this.repository          = repository;
        this.changeLogRepository = changeLogRepository;
    }

    /**
     * Returns the current electricity price.
     * GET /api/config/electricity-price
     */
    @GetMapping("/electricity-price")
    public ResponseEntity<ElectricityPriceConfig> getPrice() {
        return repository.findById(1)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates the electricity price and writes an audit log entry.
     * PATCH /api/config/electricity-price?price=29.5
     *
     * @param price       new price in ct/kWh, must be positive
     * @param userDetails injected by Spring Security – provides the username
     */
    @PatchMapping("/electricity-price")
    public ResponseEntity<ElectricityPriceConfig> updatePrice(
            @RequestParam double price,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (price <= 0) {
            return ResponseEntity.badRequest().build();
        }

        ElectricityPriceConfig config = repository.findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "Electricity price singleton missing in database"));

        // Write audit log entry before updating
        changeLogRepository.save(
                ElectricityPriceChangeLog.of(
                        config.getPriceCentPerKwh(),
                        price,
                        userDetails.getUsername()
                )
        );

        config.setPriceCentPerKwh(price);
        config.setUpdatedAt(java.time.LocalDateTime.now());
        config.setUpdatedBy(userDetails.getUsername());

        return ResponseEntity.ok(repository.save(config));
    }
}