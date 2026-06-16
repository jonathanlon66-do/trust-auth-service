package com.trust.auth.domain.service;

import com.trust.auth.domain.model.Cda;
import com.trust.auth.domain.model.User;
import com.trust.auth.domain.model.UserCda;
import com.trust.auth.domain.model.command.CreateCdaCommand;
import com.trust.auth.domain.model.result.CdaActivationResult;
import com.trust.auth.domain.port.in.CreateCdaUseCase;
import com.trust.auth.domain.port.out.CdaRepositoryPort;
import com.trust.auth.domain.port.out.CognitoPort;
import com.trust.auth.domain.port.out.EmailPort;
import com.trust.auth.domain.port.out.UserCdaRepositoryPort;
import com.trust.auth.domain.port.out.UserRepositoryPort;
import com.trust.auth.domain.exception.CompanyCodeAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCdaService implements CreateCdaUseCase {

    private final CdaRepositoryPort cdaRepository;
    private final UserRepositoryPort userRepository;
    private final UserCdaRepositoryPort userCdaRepository;
    private final CognitoPort cognito;
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
        String userId = UUID.randomUUID().toString();
        String tempPassword = generateTempPassword();

        Cda cda = Cda.create(cdaId, command.companyCode(), command.name());

        return cdaRepository.save(cda)
                .doOnSuccess(c -> log.debug("CDA guardado en DynamoDB: cdaId={}", cdaId))
                .flatMap(savedCda -> cognito.adminCreateUser(command.adminEmail(), tempPassword, command.adminName())
                        .doOnSuccess(sub -> log.debug("Usuario admin creado en Cognito: cdaId={}", cdaId))
                        .flatMap(cognitoSub -> userRepository.save(
                                User.createPending(userId, cognitoSub, command.adminEmail(), command.adminName())))
                        .doOnSuccess(u -> log.debug("Usuario guardado en DynamoDB: userId={}", userId))
                        .flatMap(savedUser -> userCdaRepository.save(
                                UserCda.createAdmin(savedUser.getUserId(), cdaId)))
                        .doOnSuccess(uc -> log.debug("Relación user↔CDA guardada: userId={}, cdaId={}", userId, cdaId))
                        .flatMap(savedUserCda -> email.sendCdaInvitation(
                                command.adminEmail(),
                                command.adminName(),
                                command.name(),
                                command.companyCode(),
                                tempPassword
                        ).doOnSuccess(v -> log.info("Email de invitación enviado a {}", command.adminEmail()))
                         .onErrorResume(e -> {
                            log.warn("No se pudo enviar el email de invitación a {} (el CDA queda activo): {}",
                                    command.adminEmail(), e.toString());
                            return Mono.empty();
                         }))
                        .thenReturn(CdaActivationResult.activated(cdaId, command.companyCode(), command.adminEmail()))
                        .onErrorResume(e -> !(e instanceof CompanyCodeAlreadyExistsException),
                                e -> {
                                    log.error("Error durante activación, ejecutando rollback del CDA cdaId={}: {}",
                                            cdaId, e.toString());
                                    return cdaRepository.delete(cdaId).then(Mono.error(e));
                                })
                );
    }

    private String generateTempPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        String base = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "Tr1!" + base.substring(0, 9);
    }
}
