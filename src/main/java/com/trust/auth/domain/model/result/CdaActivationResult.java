package com.trust.auth.domain.model.result;

public record CdaActivationResult(
        String cdaId,
        String companyCode,
        String adminEmail,
        String status
) {
    public static CdaActivationResult activated(String cdaId, String companyCode, String adminEmail) {
        return new CdaActivationResult(cdaId, companyCode, adminEmail, "ACTIVATED");
    }
}
