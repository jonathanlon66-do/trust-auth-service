package com.trust.auth.infrastructure.adapter.out.dynamodb;

import com.trust.auth.domain.model.User;
import com.trust.auth.domain.port.out.UserRepositoryPort;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.UserEntity;
import com.trust.auth.infrastructure.adapter.out.dynamodb.mapper.UserDynamoMapper;
import com.trust.auth.infrastructure.config.DynamoDbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
@RequiredArgsConstructor
public class UserDynamoAdapter implements UserRepositoryPort {

    private final DynamoDbEnhancedAsyncClient dynamoDbClient;
    private final DynamoDbProperties tables;
    private final UserDynamoMapper mapper;

    private DynamoDbAsyncTable<UserEntity> table() {
        return dynamoDbClient.table(tables.getUsers(), TableSchema.fromBean(UserEntity.class));
    }

    @Override
    public Mono<User> save(User user) {
        return Mono.fromFuture(table().putItem(mapper.toEntity(user)))
                .thenReturn(user);
    }

    @Override
    public Mono<Void> delete(String userId) {
        return Mono.fromFuture(
                table().deleteItem(Key.builder().partitionValue(userId).build())
        ).then();
    }
}
