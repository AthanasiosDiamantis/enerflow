package de.saki.enerflow.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit log entry for every electricity price change.
 * Rows are never updated or deleted after insertion.
 *
 * @author saki
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "electricity_price_change_log")
public class ElectricityPriceChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "old_price", nullable = false, columnDefinition = "NUMERIC(6,2)")
    private Double oldPrice;

    @Column(name = "new_price", nullable = false, columnDefinition = "NUMERIC(6,2)")
    private Double newPrice;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    /**
     * Convenience factory method to keep the controller clean.
     */
    public static ElectricityPriceChangeLog of(
            Double oldPrice, Double newPrice, String changedBy) {

        ElectricityPriceChangeLog log = new ElectricityPriceChangeLog();
        log.changedAt  = LocalDateTime.now();
        log.oldPrice   = oldPrice;
        log.newPrice   = newPrice;
        log.changedBy  = changedBy;
        return log;
    }
}