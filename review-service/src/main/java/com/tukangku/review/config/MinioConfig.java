package com.tukangku.review.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig implements CommandLineRunner {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    public static final String BUCKET = "review-photos";

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Override
    public void run(String... args) throws Exception {
        MinioClient client = minioClient();
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
            String policy = """
                    {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}
                    """.formatted(BUCKET);
            client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(BUCKET).config(policy).build());
            log.info("MinIO bucket '{}' created with public-read policy", BUCKET);
        }
    }
}
