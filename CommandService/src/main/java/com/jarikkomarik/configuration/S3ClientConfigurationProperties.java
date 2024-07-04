package com.jarikkomarik.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

@ConfigurationProperties(prefix = "aws.s3")
@Data
@Component
public class S3ClientConfigurationProperties {

    private Region region = Region.EU_CENTRAL_1;
    private URI endpoint = null;

    private String bucket;


    private int multipartMinPartSize = 5*1024*1024;

}