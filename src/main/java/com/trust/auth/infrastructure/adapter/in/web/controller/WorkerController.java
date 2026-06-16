package com.trust.auth.infrastructure.adapter.in.web.controller;

import com.trust.auth.domain.port.in.CreateWorkerUseCase;
import com.trust.auth.infrastructure.adapter.in.web.dto.request.CreateWorkerRequest;
import com.trust.auth.infrastructure.adapter.in.web.dto.response.CreateWorkerResponse;
import com.trust.auth.infrastructure.adapter.in.web.mapper.WorkerWebMapper;
import com.trust.auth.infrastructure.adapter.in.web.security.TrustPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class WorkerController {

    private final CreateWorkerUseCase createWorkerUseCase;
    private final WorkerWebMapper mapper;

    @PostMapping("/workers")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateWorkerResponse> createWorker(
            @AuthenticationPrincipal TrustPrincipal principal,
            @Valid @RequestBody CreateWorkerRequest request) {
        return createWorkerUseCase.execute(mapper.toCommand(principal.cdaId(), request))
                .map(mapper::toResponse);
    }
}
