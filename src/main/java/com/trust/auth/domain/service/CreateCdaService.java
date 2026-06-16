package com.trust.auth.domain.service;

import com.trust.auth.domain.model.Cda;
import com.trust.auth.domain.model.UserCda;
import com.trust.auth.domain.model.command.CreateCdaCommand;
import com.trust.auth.domain.model.result.CdaActivationResult;
import com.trust.auth.domain.port.in.CreateCdaUseCase;
import com.trust.auth.domain.port.out.CdaRepositoryPort;
import com.trust.auth.domain.port.out.EmailPort;
import com.trust.auth.domain.port.out.UserCdaRepositoryPort;
import com.trust.auth.domain.exception.CompanyCodeAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCdaService implements CreateCdaUseCase {

    private final CdaRepositoryPort cdaRepository;
    private final UserCdaRepositoryPort userCdaRepository;
    private final UserProvisioningService userProvisioning;
    private final EmailPort email;

    @Override
    public Mono<CdaActivationResult> execute(CreateCdaCommand command) {
        log.info("Activación de CDA solicitada: companyCode={}, adminEmail={}",
                command.companyCode(), command.adminEmail());
        return cdaRepository.existsByCompanyCode(command.companyCode())
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new CompanyCodeAlreadyExistsException(command.companyCode())))
                .flatMap(exists -> activateCda(command))
                .doOnSuccess(result -> log.info("CDA activado: cdaId={}, companyCode={}",
                        result.cdaId(), result.companyCode()))
                .doOnError(error -> log.error("Falló la activación del CDA companyCode={}: {}",
                        command.companyCode(), error.toString()));
    }

    private Mono<CdaActivationResult> activateCda(CreateCdaCommand command) {
        String cdaId = UUID.randomUUID().toString();
        Cda cda = Cda.create(cdaId, command.companyCode(), command.name());

        return cdaRepository.save(cda)
                .doOnSuccess(c -> log.debug("CDA guardado en DynamoDB: cdaId={}", cdaId))
                .flatMap(savedCda -> userProvisioning.provision(command.adminEmail(), command.adminName())
                        .flatMap(provisioned -> userCdaRepository.save(
                                        UserCda.createAdmin(provisioned.user().getUserId(), cdaId))
                                .doOnSuccess(uc -> log.debug("Relación user↔CDA guardada: userId={}, cdaId={}",
                                        provisioned.user().getUserId(), cdaId))
                                .flatMap(savedUserCda -> email.sendCdaInvitation(
                                                command.adminEmail(), command.adminName(), command.name(),
                                                command.companyCode(), provisioned.tempPassword())
                                        .doOnSuccess(v -> log.info("Email de invitación enviado a {}", command.adminEmail()))
                                        .onErrorResume(e -> {
                                            log.warn("No se pudo enviar el email de invitación a {} (el CDA queda activo): {}",
                                                    command.adminEmail(), e.toString());
                                            return Mono.empty();
                                        }))
                                .onErrorResume(e -> userProvisioning
                                        .deprovision(command.adminEmail(), provisioned.user().getUserId())
                                        .then(Mono.error(e))))
                        .thenReturn(CdaActivationResult.activated(cdaId, command.companyCode(), command.adminEmail())))
                .onErrorResume(e -> !(e instanceof CompanyCodeAlreadyExistsException),
                        e -> {
                            log.error("Error durante activación, rollback del CDA cdaId={}: {}", cdaId, e.toString());
                            return cdaRepository.delete(cdaId).then(Mono.error(e));
                        });
    }
}
