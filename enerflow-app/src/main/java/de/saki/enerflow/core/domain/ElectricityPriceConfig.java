package de.saki.enerflow.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Singleton entity representing the user-configured electricity price.
 * Exactly one row exists in the database at all times (id = 1).
 * Price changes are tracked separately in electricity_price_change_log.
 *
 * @author saki
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "electricity_price_config")
public class ElectricityPriceConfig {

    @Id
    private Integer id;

    @Column(name = "price_ct_kwh", nullable = false, columnDefinition = "NUMERIC(6,2)")
    private Double priceCentPerKwh;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;
}