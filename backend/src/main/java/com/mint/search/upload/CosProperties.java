package com.mint.search.upload;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cos")
public class CosProperties {
    private String bucket;
    private String region;
    private String customDomain;
    private String secretId;
    private String secretKey;
}
