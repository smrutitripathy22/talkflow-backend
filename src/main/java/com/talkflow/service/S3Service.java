package com.talkflow.service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URL;


@Service
@RequiredArgsConstructor
public class S3Service {


    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    public String uploadGoogleProfile(String googlePhotoUrl, String userEmail) {
        logger.info("Attempting to upload Google profile photo for user: {}", userEmail);
        try (InputStream in = new URL(googlePhotoUrl).openStream()) {
            byte[] imageBytes = in.readAllBytes();

            String key = "talk-flow-profile/" + userEmail.substring(0, userEmail.indexOf("@")) + "_" + java.util.UUID.randomUUID().toString() + ".jpg";
            logger.debug("Generated S3 key for profile photo: {}", key);

            s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).contentType("image/jpeg").build(), RequestBody.fromBytes(imageBytes));

            String fileUrl = baseUrl + "/" + key;
            logger.info("Successfully uploaded profile photo for user {} to URL: {}", userEmail, fileUrl);
            return fileUrl;

        } catch (Exception e) {
            logger.error("Failed to upload profile image for user: {}", userEmail, e);
            throw new RuntimeException("Failed to upload profile image", e);
        }
    }

    public String uploadProfileWithUuid(InputStream inputStream, long contentLength, String userEmail,
                                        String contentType, String existingUrl) {
        try {

            if (existingUrl != null && !existingUrl.isBlank()) {
                String oldKey = existingUrl.replace(baseUrl + "/", "");
                logger.debug("Deleting old profile image with key: {}", oldKey);

                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(oldKey)
                        .build());

                logger.info("Deleted old profile image for user {}", userEmail);
            }


            String key = "talk-flow-profile/"
                    + userEmail.substring(0, userEmail.indexOf("@"))
                    + "_" + java.util.UUID.randomUUID().toString()
                    + ".jpg";

            logger.debug("Uploading new profile image for user {} with key: {}", userEmail, key);

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, contentLength)
            );

            String fileUrl = baseUrl + "/" + key;
            logger.info("Profile photo uploaded for user {} at URL: {}", userEmail, fileUrl);

            return fileUrl;

        } catch (Exception e) {
            logger.error("Failed to upload/replace profile image for user: {}", userEmail, e);
            throw new RuntimeException("Failed to upload/replace profile image", e);
        }
    }


}

