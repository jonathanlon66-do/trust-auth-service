package com.trust.auth.infrastructure.adapter.out.cognito;

import com.trust.auth.domain.port.out.CognitoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;

@Slf4j
@Component
@RequiredArgsConstructor
public class CognitoAdapter implements CognitoPort {

    private final CognitoIdentityProviderAsyncClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Override
    public Mono<String> adminCreateUser(String email, String tempPassword, String name) {
        return Mono.fromFuture(cognitoClient.adminCreateUser(AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .temporaryPassword(tempPassword)
                .messageAction(MessageActionType.SUPPRESS)
                .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                .userAttributes(
                        AttributeType.builder().name("email").value(email).build(),
                        AttributeType.builder().name("name").value(name).build(),
                        AttributeType.builder().name("email_verified").value("true").build()
                )
                .build()))
                .map(response -> response.user().attributes().stream()
                        .filter(a -> a.name().equals("sub"))
                        .findFirst()
                        .map(AttributeType::value)
                        .orElseThrow(() -> new RuntimeException("Cognito no retornó el sub del usuario")))
                .doOnError(e -> log.error("Error creando usuario en Cognito (email={}): {}", email, e.toString()));
    }

    @Override
    public Mono<Void> deleteUser(String email) {
        return Mono.fromFuture(cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build())).then();
    }
}
