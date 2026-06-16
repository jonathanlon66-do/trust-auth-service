package com.trust.auth.domain.port.out;

import reactor.core.publisher.Mono;

public interface EmailPort {

    Mono<Void> sendCdaInvitation(String toEmail, String adminName, String cdaName,
                                  String companyCode, String tempPassword);

    Mono<Void> sendWorkerInvitation(String toEmail, String workerName, String cdaName,
                                     String companyCode, String tempPassword);

    Mono<Void> sendCdaAdded(String toEmail, String workerName, String cdaName, String companyCode);
}
