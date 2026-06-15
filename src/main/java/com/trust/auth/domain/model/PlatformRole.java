package com.trust.auth.domain.model;

/**
 * Rol a nivel de plataforma Trust, independiente de cualquier CDA.
 * Un usuario normal de un CDA tiene platformRole = null.
 * Solo el staff interno de Trust tiene un PlatformRole.
 */
public enum PlatformRole {
    TRUST_ADMIN
}
