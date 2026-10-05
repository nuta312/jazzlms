package kz.jazz.lms.course.storage;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Объектное хранилище (MinIO = S3-совместимый API).
 *
 * Почему не в PostgreSQL и не на диск сервиса:
 *  - видео на сотни мегабайт раздувают БД и бэкапы;
 *  - у сервиса может быть несколько экземпляров, локальный диск у каждого свой.
 *
 * Как файл попадает к ученику: сервис НЕ гоняет байты через себя. Он выдаёт presigned URL —
 * ссылку с подписью и сроком жизни. Браузер качает файл напрямую из MinIO (с поддержкой Range,
 * поэтому видео можно перематывать), а без подписи бакет закрыт.
 */
@Component
@EnableConfigurationProperties(StorageProperties.class)
public class FileStorage {
    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);
    private static final String REGION = "us-east-1";

    private final StorageProperties props;
    private final MinioClient client;        // ходит в MinIO по внутреннему адресу
    private final MinioClient presigner;     // только считает подпись для публичного адреса (в сеть не ходит)
    private volatile boolean bucketReady = false;

    public FileStorage(StorageProperties props) {
        this.props = props;
        this.client = MinioClient.builder().endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey()).region(REGION).build();
        this.presigner = MinioClient.builder().endpoint(props.publicUrl())
                .credentials(props.accessKey(), props.secretKey()).region(REGION).build();
    }

    /** Кладёт файл в бакет, возвращает ключ объекта. */
    public String upload(UUID courseId, MultipartFile file) {
        ensureBucket();
        String safeName = file.getOriginalFilename() == null ? "file"
                : file.getOriginalFilename().replaceAll("[^A-Za-z0-9._-]", "_");
        String key = "courses/" + courseId + "/" + UUID.randomUUID() + "-" + safeName;
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket()).object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
            log.info("Uploaded {} ({} bytes) to s3://{}/{}", safeName, file.getSize(), props.bucket(), key);
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("File storage unavailable: " + e.getMessage(), e);
        }
    }

    /** Временная ссылка на скачивание/просмотр. */
    public String presignedUrl(String key) {
        try {
            return presigner.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET).bucket(props.bucket()).object(key)
                    .expiry(props.presignMinutes(), TimeUnit.MINUTES).build());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot presign URL: " + e.getMessage(), e);
        }
    }

    /** Копия объекта внутри бакета (server-side copy: байты не проходят через сервис). */
    public String copy(String sourceKey, UUID targetCourseId) {
        ensureBucket();
        String name = sourceKey.substring(sourceKey.lastIndexOf('-') + 1);
        String key = "courses/" + targetCourseId + "/" + UUID.randomUUID() + "-" + name;
        try {
            client.copyObject(CopyObjectArgs.builder().bucket(props.bucket()).object(key)
                    .source(CopySource.builder().bucket(props.bucket()).object(sourceKey).build()).build());
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot copy file: " + e.getMessage(), e);
        }
    }

    public void delete(String key) {
        if (key == null) return;
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(props.bucket()).object(key).build());
        } catch (Exception e) {
            log.warn("Could not delete object {}: {}", key, e.getMessage());
        }
    }

    private void ensureBucket() {
        if (bucketReady) return;
        synchronized (this) {
            if (bucketReady) return;
            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(props.bucket()).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
                    log.info("Created bucket {}", props.bucket());
                }
                bucketReady = true;
            } catch (Exception e) {
                throw new IllegalStateException("File storage unavailable: " + e.getMessage(), e);
            }
        }
    }
}
