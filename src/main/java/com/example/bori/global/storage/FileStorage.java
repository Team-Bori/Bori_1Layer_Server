package com.example.bori.global.storage;

import java.io.InputStream;

public interface FileStorage {

    // 저장하고 공개 URL을 돌려준다.
    String upload(String key, InputStream content, long size, String contentType);

    // 공개 URL로 지운다. 이 저장소 URL이 아니면 무시한다.
    void deleteByUrl(String url);
}
