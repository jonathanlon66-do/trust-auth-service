package com.trust.auth.infrastructure.adapter.in.web.controller;

import com.trust.auth.domain.port.in.CreateCdaUseCase;
import com.trust.auth.infrastructure.adapter.in.web.dto.request.CreateCdaRequest;
import com.trust.auth.infrastructure.adapter.in.web.dto.response.CreateCdaResponse;
import com.trust.auth.infrastructure.adapter.in.web.mapper.CdaWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class OnboardingController {

    private final CreateCdaUseCase createCdaUseCase;
    private final CdaWebMapper mapper;

    @PostMapping("/cdas")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateCdaResponse> activateCda(@Valid @RequestBody CreateCdaRequest request) {
        return createCdaUseCase.execute(mapper.toCommand(request))
                .map(mapper::toResponse);
    }
}
