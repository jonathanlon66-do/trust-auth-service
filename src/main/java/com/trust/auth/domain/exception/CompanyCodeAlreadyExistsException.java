package com.trust.auth.domain.exception;

public class CompanyCodeAlreadyExistsException extends RuntimeException {
    public CompanyCodeAlreadyExistsException(String companyCode) {
        super("El código de empresa ya existe: " + companyCode);
    }
}
