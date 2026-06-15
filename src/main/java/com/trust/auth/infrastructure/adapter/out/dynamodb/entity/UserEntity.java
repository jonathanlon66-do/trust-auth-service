package com.trust.auth.infrastructure.adapter.out.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class UserEntity {

    private String userId;
    private String cognitoSub;
    private String email;
    private String name;
    private String phone;
    private String documentNumber;
    private boolean onboarded;
    private boolean active;
    private String createdAt;

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }
}
