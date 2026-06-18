// core/domain/UserRole.java
package de.saki.enerflow.core.domain;

/**
 * Role of an EnerFlow user, used for role-based access control (AP3.3).
 *
 * @author saki
 */
public enum UserRole {
    ROLE_USER,
    ROLE_MANAGER,
    ROLE_ADMIN
}