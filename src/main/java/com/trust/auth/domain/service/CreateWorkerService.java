package com.trust.auth.domain.service;

import com.trust.auth.domain.exception.WorkerAlreadyInCdaException;
import com.trust.auth.domain.model.User;
import com.trust.auth.domain.model.UserCda;
import com.trust.auth.domain.model.command.CreateWorkerCommand;
import com.trust.auth.domain.model.result.WorkerCreatedResult;
import com.trust.auth.domain.port.in.CreateWorkerUseCase;
import com.trust.auth.domain.port.out.CdaRepositoryPort;
import com.trust.auth.domain.port.out.EmailPort;
import com.trust.auth.domain.port.out.UserCdaRepositoryPort;
import com.trust.auth.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateWorkerService implements CreateWorkerUseCase {

    private final UserRepositoryPort userRepository;
    private final UserCdaRepositoryPort userCdaRepository;
    private final CdaRepositoryPort cdaRepository;
    private final UserProvisioningService userProvisioning;
    private final EmailPort email;

    @Override
    public Mono<WorkerCreatedResult> execute(CreateWorkerCommand command) {
        log.info("Creación de trabajador solicitada: cdaId={}, email={}", command.cdaId(), command.email());
        return userRepository.findByEmail(command.email())
                .flatMap(existing -> linkExistingUser(command, existing))
                .switchIfEmpty(Mono.defer(() -> createNewUser(command)))
                .doOnSuccess(r -> log.info("Trabajador creado: userId={}, cdaId={}", r.userId(), command.cdaId()))
                .doOnError(e -> log.error("Falló creación de trabajador email={}: {}", command.email(), e.toString()));
    }

    private Mono<WorkerCreatedResult> createNewUser(CreateWorkerCommand command) {
        return userProvisioning.provision(command.email(), command.name())
                .flatMap(provisioned -> {
                    String userId = provisioned.user().getUserId();
                    return userCdaRepository.save(
                                    UserCda.createWorker(userId, command.cdaId(), command.roleName(), command.scopes()))
                            .flatMap(uc -> sendWorkerInvitation(command, provisioned.tempPassword()).thenReturn(uc))
                            .onErrorResume(e -> userProvisioning.deprovision(command.email(), userId).then(Mono.error(e)))
                            .thenReturn(WorkerCreatedResult.invited(userId, command.email(), command.roleName(), command.scopes()));
                });
    }

    private Mono<WorkerCreatedResult> linkExistingUser(CreateWorkerCommand command, User existing) {
        return userCdaRepository.exists(command.cdaId(), existing.getUserId())
                .flatMap(alreadyIn -> Boolean.TRUE.equals(alreadyIn)
                        ? Mono.error(new WorkerAlreadyInCdaException(command.email()))
                        : userCdaRepository.save(UserCda.createWorker(
                                existing.getUserId(), command.cdaId(), command.roleName(), command.scopes()))
                            .flatMap(uc -> sendCdaAdded(command).thenReturn(uc)))
                .thenReturn(WorkerCreatedResult.invited(
                        existing.getUserId(), command.email(), command.roleName(), command.scopes()));
    }

    private Mono<Void> sendWorkerInvitation(CreateWorkerCommand command, String tempPassword) {
        return cdaRepository.findById(command.cdaId())
                .flatMap(cda -> email.sendWorkerInvitation(
                        command.email(), command.name(), cda.getName(), cda.getCompanyCode(), tempPassword))
                .doOnSuccess(v -> log.info("Email worker-invitation enviado a {}", command.email()))
                .onErrorResume(e -> {
                    log.warn("No se pudo enviar worker-invitation a {}: {}", command.email(), e.toString());
                    return Mono.empty();
                });
    }

    private Mono<Void> sendCdaAdded(CreateWorkerCommand command) {
        return cdaRepository.findById(command.cdaId())
                .flatMap(cda -> email.sendCdaAdded(
                        command.email(), command.name(), cda.getName(), cda.getCompanyCode()))
                .doOnSuccess(v -> log.info("Email cda-added enviado a {}", command.email()))
                .onErrorResume(e -> {
                    log.warn("No se pudo enviar cda-added a {}: {}", command.email(), e.toString());
                    return Mono.empty();
                });
    }
}
