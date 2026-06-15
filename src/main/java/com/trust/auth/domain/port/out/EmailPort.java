package com.trust.auth.domain.port.out;

import reactor.core.publisher.Mono;

public interface EmailPort {
    Mono<Void> sendCdaInvitation(String toEmail, String adminName, String cdaName,
                                  String companyCode, String tempPassword);
}
