package kz.jazz.lms.course.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param endpoint  адрес MinIO для самого сервиса (в docker: http://minio:9000)
 * @param publicUrl адрес MinIO, каким его видит БРАУЗЕР (http://localhost:9002) — для presigned-ссылок
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String endpoint, String publicUrl, String accessKey, String secretKey,
                                String bucket, int presignMinutes) {}
