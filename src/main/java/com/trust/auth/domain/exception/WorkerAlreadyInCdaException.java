package com.trust.auth.domain.exception;

public class WorkerAlreadyInCdaException extends RuntimeException {
    public WorkerAlreadyInCdaException(String email) {
        super("Este usuario ya es parte de tu empresa: " + email);
    }
}
