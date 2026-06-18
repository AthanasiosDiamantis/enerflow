package de.saki.enerflow.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a single polling snapshot of the sonnenBatterie.
 * Stored every 60 seconds (configurable) in the database, mirroring
 * {@link HeatpumpSnapshot}.
 *
 * @author saki
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "battery_snapshot")
public class BatterySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "usable_soc_percent", nullable = false)
    private Integer usableSocPercent;

    @Column(name = "production_watts", nullable = false)
    private Integer productionWatts;

    @Column(name = "consumption_watts", nullable = false)
    private Integer consumptionWatts;

    @Column(name = "battery_power_watts", nullable = false)
    private Integer batteryPowerWatts;

    @Column(name = "battery_charging", nullable = false)
    private boolean batteryCharging;

    @Column(name = "battery_discharging", nullable = false)
    private boolean batteryDischarging;

    @Column(name = "surplus_watts", nullable = false)
    private Integer surplusWatts;
}
