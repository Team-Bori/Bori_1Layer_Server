package com.example.bori.global.storage;

import com.example.bori.global.error.BusinessException;
import com.example.bori.global.error.ErrorCode;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public FileStorage fileStorage(StorageProperties props) {
        if (props.enabled()) {
            log.info("사진 저장소: S3 bucket={} url={}", props.bucket(), props.resolvedPublicBaseUrl());
            return new S3FileStorage(props);
        }
        log.warn("S3_BUCKET이 없어 사진 업로드를 끈다.");
        return new FileStorage() {
            @Override
            public String upload(String key, InputStream content, long size, String contentType) {
                throw new BusinessException(ErrorCode.STORAGE_NOT_CONFIGURED);
            }

            @Override
            public void deleteByUrl(String url) {
            }
        };
    }
}
