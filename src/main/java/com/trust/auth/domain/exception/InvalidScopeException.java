package com.trust.auth.domain.exception;

public class InvalidScopeException extends RuntimeException {
    public InvalidScopeException(String scope) {
        super("Scope no reconocido: " + scope);
    }
}
