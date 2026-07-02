package com.smartcbwtf.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {
    public static final int DEFAULT_MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable pageRequest(int page, int size, int defaultSize, Sort sort) {
        return PageRequest.of(normalizePage(page), normalizeSize(size, defaultSize, DEFAULT_MAX_PAGE_SIZE), sort);
    }

    public static Pageable pageRequest(int page, int size, int defaultSize, int maxSize, Sort sort) {
        return PageRequest.of(normalizePage(page), normalizeSize(size, defaultSize, maxSize), sort);
    }

    public static Pageable pageRequest(int page, int size, int defaultSize) {
        return PageRequest.of(normalizePage(page), normalizeSize(size, defaultSize, DEFAULT_MAX_PAGE_SIZE));
    }

    public static int normalizePage(int page) {
        return Math.max(page, 0);
    }

    public static int normalizeSize(int size, int defaultSize, int maxSize) {
        int safeDefault = Math.max(defaultSize, 1);
        int safeMax = Math.max(maxSize, safeDefault);
        if (size < 1) {
            return safeDefault;
        }
        return Math.min(size, safeMax);
    }
}
