package com.example.bori.global.storage;

import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.io.InputStream;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// ACL은 지정하지 않는다. ACL이 꺼진 버킷(기본값)은 ACL 지정 PutObject를 거절한다.
// 공개 읽기는 버킷 정책(예: rental-boards/* GetObject 허용)이나 CloudFront로 연다.
// 자격 증명은 SDK 기본 체인(환경변수 AWS_ACCESS_KEY_ID/SECRET, EC2 인스턴스 역할 등)을 쓴다.
@Slf4j
public class S3FileStorage implements FileStorage {

    private final S3Client client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3FileStorage(StorageProperties props) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(props.region()));
        if (props.endpoint() != null && !props.endpoint().isBlank()) {
            // MinIO 등 S3 호환 저장소
            builder.endpointOverride(URI.create(props.endpoint())).forcePathStyle(true);
        }
        this.client = builder.build();
        this.bucket = props.bucket();
        this.publicBaseUrl = props.resolvedPublicBaseUrl();
    }

    @Override
    public String upload(String key, InputStream content, long size, String contentType) {
        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromInputStream(content, size));
            return publicBaseUrl + "/" + key;
        } catch (SdkException e) {
            log.error("S3 업로드 실패 {}: {}", key, e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteByUrl(String url) {
        if (url == null || !url.startsWith(publicBaseUrl + "/")) {
            return;
        }
        String key = url.substring(publicBaseUrl.length() + 1);
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("S3 삭제 실패 {}: {}", key, e.getMessage());
        }
    }
}
