package com.trust.auth.infrastructure.adapter.out.dynamodb.mapper;

import com.trust.auth.domain.model.Scope;
import com.trust.auth.domain.model.UserCda;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.UserCdaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserCdaDynamoMapper {

    @Mapping(target = "scopes", expression = "java(userCda.getScopes().stream().map(com.trust.auth.domain.model.Scope::name).toList())")
    @Mapping(target = "createdAt", expression = "java(userCda.getCreatedAt().toString())")
    UserCdaEntity toEntity(UserCda userCda);
}
