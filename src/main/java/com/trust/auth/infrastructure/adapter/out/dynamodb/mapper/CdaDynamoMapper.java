package com.trust.auth.infrastructure.adapter.out.dynamodb.mapper;

import com.trust.auth.domain.model.Cda;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.CdaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface CdaDynamoMapper {

    @Mapping(target = "createdAt", expression = "java(cda.getCreatedAt().toString())")
    CdaEntity toEntity(Cda cda);

    @Mapping(target = "createdAt", expression = "java(parseInstant(entity.getCreatedAt()))")
    Cda toDomain(CdaEntity entity);

    default Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
