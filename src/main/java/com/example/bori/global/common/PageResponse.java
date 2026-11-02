package com.example.bori.global.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

// Spring Data Page를 그대로 직렬화하면 구조가 버전마다 달라질 수 있어 고정된 모양으로 내보낸다.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
