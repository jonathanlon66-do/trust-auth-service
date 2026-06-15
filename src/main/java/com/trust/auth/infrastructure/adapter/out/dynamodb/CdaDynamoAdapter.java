package com.trust.auth.infrastructure.adapter.out.dynamodb;

import com.trust.auth.domain.model.Cda;
import com.trust.auth.domain.port.out.CdaRepositoryPort;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.CdaEntity;
import com.trust.auth.infrastructure.adapter.out.dynamodb.mapper.CdaDynamoMapper;
import com.trust.auth.infrastructure.config.DynamoDbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Component
@RequiredArgsConstructor
public class CdaDynamoAdapter implements CdaRepositoryPort {

    private final DynamoDbEnhancedClient dynamoDbClient;
    private final DynamoDbProperties tables;
    private final CdaDynamoMapper mapper;

    private DynamoDbTable<CdaEntity> table() {
        return dynamoDbClient.table(tables.getCdas(), TableSchema.fromBean(CdaEntity.class));
    }

    @Override
    public Mono<Boolean> existsByCompanyCode(String companyCode) {
        return Mono.fromCallable(() -> {
            DynamoDbIndex<CdaEntity> index = table().index("company_code-index");
            var results = index.query(QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(companyCode).build()
            ));
            return results.stream().anyMatch(page -> !page.items().isEmpty());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Cda> save(Cda cda) {
        return Mono.fromCallable(() -> {
            table().putItem(mapper.toEntity(cda));
            return cda;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(String cdaId) {
        return Mono.fromRunnable(() ->
                table().deleteItem(Key.builder().partitionValue(cdaId).build())
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
