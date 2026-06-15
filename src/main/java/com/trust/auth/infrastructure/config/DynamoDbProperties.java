package com.trust.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dynamodb.tables")
public class DynamoDbProperties {
    private String users;
    private String cdas;
    private String userCda;
}
