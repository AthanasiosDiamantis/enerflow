package de.saki.enerflow.adapter.web.dto;

import de.saki.enerflow.core.service.DeviceConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes runtime-configurable EnerFlow parameters for the Manager UI.
 * Read access requires MANAGER or ADMIN; write access likewise.
 *
 * @author saki
 */
@RestController
@RequestMapping("/api/config/device")
public class DeviceConfigController {

    private final DeviceConfigService deviceConfigService;

    public DeviceConfigController(DeviceConfigService deviceConfigService) {
        this.deviceConfigService = deviceConfigService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public DeviceConfigDto getConfig() {
        return deviceConfigService.getAll();
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Object> updateConfig(@RequestBody DeviceConfigDto dto) {
        if (dto.pvSurplusThresholdWatts() <= 0
                || dto.batterySocThresholdPercent() < 0 || dto.batterySocThresholdPercent() > 100
                || dto.hotwaterTankVolumeLiters() <= 0
                || dto.snapshotRetentionDays() <= 0) {
            return ResponseEntity.badRequest().body("Invalid configuration values.");
        }
        if (dto.hotwaterSetpointElevatedCelsius() <= dto.hotwaterSetpointNormalCelsius()) {
            return ResponseEntity.badRequest()
                    .body("Elevated setpoint must be higher than the normal setpoint.");
        }

        deviceConfigService.updateConfig(dto);
        return ResponseEntity.ok(deviceConfigService.getAll());
    }
}