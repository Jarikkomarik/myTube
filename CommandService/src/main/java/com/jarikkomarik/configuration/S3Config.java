package com.jarikkomarik.configuration;

import io.micrometer.common.util.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;


@Configuration
public class S3Config {

    @Bean
    public S3AsyncClient s3client(S3ClientConfigurationProperties s3props) {
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ZERO)
                .maxConcurrency(64)
                .build();
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(false)
                .chunkedEncodingEnabled(true)
                .build();
        S3AsyncClientBuilder b = S3AsyncClient.builder().httpClient(httpClient)
                .region(s3props.getRegion())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .serviceConfiguration(serviceConfiguration);

        if (s3props.getEndpoint() != null) {
            b = b.endpointOverride(s3props.getEndpoint());
        }
        return b.build();
    }
}
