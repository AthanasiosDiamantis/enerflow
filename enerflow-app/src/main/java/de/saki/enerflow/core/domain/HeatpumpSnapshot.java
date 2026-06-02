package de.saki.enerflow.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a single polling snapshot of the heat pump.
 * Stored every 60 seconds (configurable) in the database.
 *
 * @author saki
 */
@Entity
@Table(name = "heatpump_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class HeatpumpSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // --- Temperatures (from "Informationen → Temperaturen") ---

    @Column(name = "vorlauf_temp")
    private Double vorlaufTemp;

    @Column(name = "ruecklauf_temp")
    private Double ruecklaufTemp;

    @Column(name = "warmwasser_ist")
    private Double warmwasserIst;

    @Column(name = "warmwasser_soll")
    private Double warmwasserSoll;

    @Column(name = "aussentemperatur")
    private Double aussentemperatur;

    // --- Operating data (from "Anlagenstatus") ---

    @Column(name = "heizleistung_kw")
    private Double heizleistungKw;

    @Column(name = "leistungsaufnahme_kw")
    private Double leistungsaufnahmeKw;

    @Column(name = "betriebszustand")
    private String betriebszustand;

    @Column(name = "betriebsstunden_wp")
    private Integer betriebsstundenWp;

    // --- Settings (from "Einstellungen → Temperaturen") ---

    @Column(name = "hysterese_ww_k")
    private Double hystereseWwK;
}
