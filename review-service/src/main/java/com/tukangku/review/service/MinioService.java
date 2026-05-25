package com.tukangku.review.service;

import com.tukangku.review.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.public-url}")
    private String publicUrl;

    /**
     * Upload satu foto ke bucket review-photos dan return URL publiknya.
     *
     * @param file       file yang diupload
     * @param objectName nama unik objek di MinIO (termasuk prefix folder jika perlu)
     * @return URL publik yang bisa langsung diakses browser
     */
    public String uploadPhoto(MultipartFile file, String objectName) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(MinioConfig.BUCKET)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return publicUrl + "/" + MinioConfig.BUCKET + "/" + objectName;
    }
}
