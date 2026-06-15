package com.trust.auth.infrastructure.adapter.out.dynamodb;

import com.trust.auth.domain.model.User;
import com.trust.auth.domain.port.out.UserRepositoryPort;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.UserEntity;
import com.trust.auth.infrastructure.adapter.out.dynamodb.mapper.UserDynamoMapper;
import com.trust.auth.infrastructure.config.DynamoDbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
@RequiredArgsConstructor
public class UserDynamoAdapter implements UserRepositoryPort {

    private final DynamoDbEnhancedClient dynamoDbClient;
    private final DynamoDbProperties tables;
    private final UserDynamoMapper mapper;

    private DynamoDbTable<UserEntity> table() {
        return dynamoDbClient.table(tables.getUsers(), TableSchema.fromBean(UserEntity.class));
    }

    @Override
    public Mono<User> save(User user) {
        return Mono.fromCallable(() -> {
            table().putItem(mapper.toEntity(user));
            return user;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(String userId) {
        return Mono.fromRunnable(() ->
                table().deleteItem(Key.builder().partitionValue(userId).build())
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
