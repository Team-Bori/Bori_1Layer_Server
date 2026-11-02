package com.example.bori.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

// bucket이 비어 있으면 사진 업로드를 쓸 수 없다 (나머지 기능은 그대로 동작).
@ConfigurationProperties("bori.storage.s3")
public record StorageProperties(
        String bucket,
        String region,
        String endpoint,
        String publicBaseUrl
) {

    public boolean enabled() {
        return bucket != null && !bucket.isBlank();
    }

    // 지정이 없으면 S3 가상 호스트 URL (엔드포인트를 바꿨으면 path-style)
    public String resolvedPublicBaseUrl() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "");
        }
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint.replaceAll("/+$", "") + "/" + bucket;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com";
    }
}
