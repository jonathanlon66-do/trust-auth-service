package com.trust.auth.infrastructure.adapter.out.dynamodb;

import com.trust.auth.domain.model.Cda;
import com.trust.auth.domain.port.out.CdaRepositoryPort;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.CdaEntity;
import com.trust.auth.infrastructure.adapter.out.dynamodb.mapper.CdaDynamoMapper;
import com.trust.auth.infrastructure.config.DynamoDbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Component
@RequiredArgsConstructor
public class CdaDynamoAdapter implements CdaRepositoryPort {

    private final DynamoDbEnhancedAsyncClient dynamoDbClient;
    private final DynamoDbProperties tables;
    private final CdaDynamoMapper mapper;

    private DynamoDbAsyncTable<CdaEntity> table() {
        return dynamoDbClient.table(tables.getCdas(), TableSchema.fromBean(CdaEntity.class));
    }

    @Override
    public Mono<Boolean> existsByCompanyCode(String companyCode) {
        var index = table().index("company_code-index");
        var query = QueryConditional.keyEqualTo(Key.builder().partitionValue(companyCode).build());
        return Mono.from(index.query(r -> r.queryConditional(query)))
                .map(page -> !page.items().isEmpty())
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Cda> findById(String cdaId) {
        return Mono.fromFuture(table().getItem(Key.builder().partitionValue(cdaId).build()))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Cda> save(Cda cda) {
        return Mono.fromFuture(table().putItem(mapper.toEntity(cda)))
                .thenReturn(cda);
    }

    @Override
    public Mono<Void> delete(String cdaId) {
        return Mono.fromFuture(
                table().deleteItem(Key.builder().partitionValue(cdaId).build())
        ).then();
    }
}
