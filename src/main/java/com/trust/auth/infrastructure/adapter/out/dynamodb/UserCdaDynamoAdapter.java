package com.trust.auth.infrastructure.adapter.out.dynamodb;

import com.trust.auth.domain.model.UserCda;
import com.trust.auth.domain.port.out.UserCdaRepositoryPort;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.UserCdaEntity;
import com.trust.auth.infrastructure.adapter.out.dynamodb.mapper.UserCdaDynamoMapper;
import com.trust.auth.infrastructure.config.DynamoDbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
@RequiredArgsConstructor
public class UserCdaDynamoAdapter implements UserCdaRepositoryPort {

    private final DynamoDbEnhancedClient dynamoDbClient;
    private final DynamoDbProperties tables;
    private final UserCdaDynamoMapper mapper;

    private DynamoDbTable<UserCdaEntity> table() {
        return dynamoDbClient.table(tables.getUserCda(), TableSchema.fromBean(UserCdaEntity.class));
    }

    @Override
    public Mono<UserCda> save(UserCda userCda) {
        return Mono.fromCallable(() -> {
            table().putItem(mapper.toEntity(userCda));
            return userCda;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
