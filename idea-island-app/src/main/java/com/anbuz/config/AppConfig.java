package com.anbuz.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 应用基础配置，集中提供跨模块共享的通用 Bean。
 */
@Configuration
public class AppConfig {

    @Value("${cos.secret-id}")
    private String cosSecretId;

    @Value("${cos.secret-key}")
    private String cosSecretKey;

    @Value("${cos.region}")
    private String cosRegion;

    @Bean
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(cosSecretId, cosSecretKey);
        ClientConfig config = new ClientConfig(new Region(cosRegion));
        return new COSClient(cred, config);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
