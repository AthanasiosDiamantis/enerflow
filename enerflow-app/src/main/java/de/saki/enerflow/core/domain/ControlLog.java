package de.saki.enerflow.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit log entry for every setpoint change EnerFlow performs
 * automatically (RAISE/RESET) or a user performs manually (MANUAL).
 * Rows are never updated or deleted after insertion.
 *
 * @author saki
 */
@Entity
@Table(name = "control_log")
@Getter
@Setter
@NoArgsConstructor
public class ControlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ControlAction action;

    @Column(name = "old_setpoint_celsius")
    private Double oldSetpointCelsius;

    @Column(name = "new_setpoint_celsius")
    private Double newSetpointCelsius;

    @Column(name = "berechnung_kwh")
    private Double berechnungKwh;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;
}
