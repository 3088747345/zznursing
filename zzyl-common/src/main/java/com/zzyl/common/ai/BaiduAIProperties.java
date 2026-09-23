package com.zzyl.common.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class BaiduAIProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
}