package com.trust.auth.infrastructure.adapter.out.dynamodb.mapper;

import com.trust.auth.domain.model.User;
import com.trust.auth.infrastructure.adapter.out.dynamodb.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface UserDynamoMapper {

    @Mapping(target = "createdAt", expression = "java(user.getCreatedAt().toString())")
    UserEntity toEntity(User user);

    @Mapping(target = "createdAt", expression = "java(parseInstant(entity.getCreatedAt()))")
    User toDomain(UserEntity entity);

    default Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
