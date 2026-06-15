package com.trust.auth.domain.service;

import com.trust.auth.domain.model.Cda;
import com.trust.auth.domain.model.Scope;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

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
        return cdaRepository.existsByCompanyCode(command.companyCode())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new CompanyCodeAlreadyExistsException(command.companyCode()));
                    }
                    return activateCda(command);
                });
    }

    private Mono<CdaActivationResult> activateCda(CreateCdaCommand command) {
        String cdaId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String tempPassword = generateTempPassword();

        Cda cda = Cda.builder()
                .cdaId(cdaId)
                .companyCode(command.companyCode())
                .name(command.name())
                .active(true)
                .createdAt(Instant.now())
                .build();

        return cdaRepository.save(cda)
                .flatMap(savedCda -> cognito.adminCreateUser(command.adminEmail(), tempPassword, command.adminName())
                        .flatMap(cognitoSub -> {
                            User user = User.builder()
                                    .userId(userId)
                                    .cognitoSub(cognitoSub)
                                    .email(command.adminEmail())
                                    .name(command.adminName())
                                    .onboarded(false)
                                    .active(true)
                                    .createdAt(Instant.now())
                                    .build();

                            return userRepository.save(user);
                        })
                        .flatMap(savedUser -> {
                            UserCda userCda = UserCda.builder()
                                    .userId(savedUser.getUserId())
                                    .cdaId(cdaId)
                                    .role("Administrador")
                                    .scopes(List.of(Scope.values()))
                                    .active(true)
                                    .createdAt(Instant.now())
                                    .build();

                            return userCdaRepository.save(userCda);
                        })
                        .flatMap(savedUserCda -> email.sendCdaInvitation(
                                command.adminEmail(),
                                command.adminName(),
                                command.name(),
                                command.companyCode(),
                                tempPassword
                        ).onErrorResume(e -> Mono.empty()))
                        .thenReturn(CdaActivationResult.activated(cdaId, command.companyCode(), command.adminEmail()))
                        .onErrorResume(e -> !(e instanceof CompanyCodeAlreadyExistsException),
                                e -> cdaRepository.delete(cdaId).then(Mono.error(e)))
                );
    }

    private String generateTempPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        String base = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "Tr!" + base.substring(0, 9);
    }
}
