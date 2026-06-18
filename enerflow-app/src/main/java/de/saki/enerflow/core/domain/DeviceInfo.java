// core/domain/DeviceInfo.java
package de.saki.enerflow.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Read-only hardware metadata reported by the connected devices themselves
 * (serial number, firmware version, IP address). Distinct from
 * {@link DeviceConfig}, which holds user-configurable parameters.
 *
 * @author saki
 */
@Getter
@Setter
@Entity
@Table(name = "device_info")
public class DeviceInfo {

    @Id
    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}