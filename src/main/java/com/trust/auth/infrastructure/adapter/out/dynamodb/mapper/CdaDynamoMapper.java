package com.trust.auth.infrastructure.adapter.out.dynamodb.mapper;

import com.trust.auth.domain.model.Cda;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.CdaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CdaDynamoMapper {

    @Mapping(target = "createdAt", expression = "java(cda.getCreatedAt().toString())")
    CdaEntity toEntity(Cda cda);
}
