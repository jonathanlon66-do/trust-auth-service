package com.trust.auth.infrastructure.adapter.out.dynamodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class CdaEntity {

    private String cdaId;
    private String companyCode;
    private String name;
    private boolean active;
    private String createdAt;

    @DynamoDbPartitionKey
    public String getCdaId() { return cdaId; }

    @DynamoDbSecondaryPartitionKey(indexNames = "company_code-index")
    public String getCompanyCode() { return companyCode; }
}
