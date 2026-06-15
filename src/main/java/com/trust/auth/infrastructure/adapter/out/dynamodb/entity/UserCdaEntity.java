package com.trust.auth.infrastructure.adapter.out.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class UserCdaEntity {

    private String cdaId;
    private String userId;
    private String role;
    private List<String> scopes;
    private boolean active;
    private String createdAt;

    @DynamoDbPartitionKey
    public String getCdaId() { return cdaId; }

    @DynamoDbSortKey
    public String getUserId() { return userId; }
}
