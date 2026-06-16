package com.trust.auth.domain.port.in;

import com.trust.auth.domain.model.command.CreateWorkerCommand;
import com.trust.auth.domain.model.result.WorkerCreatedResult;
import reactor.core.publisher.Mono;

public interface CreateWorkerUseCase {
    Mono<WorkerCreatedResult> execute(CreateWorkerCommand command);
}
