package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.command.CreateCdaCommand;
import com.trust.auth.domain.model.result.CdaActivationResult;
import reactor.core.publisher.Mono;

public interface CreateCdaUseCase {
    Mono<CdaActivationResult> execute(CreateCdaCommand command);
}
