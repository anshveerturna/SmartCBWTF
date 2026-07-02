package com.smartcbwtf.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationUtilsTest {

    @Test
    void pageRequestNormalizesNegativePageAndOversizedPageSize() {
        Pageable pageable = PaginationUtils.pageRequest(-5, 5_000, 20, Sort.by("createdAt"));

        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
    }

    @Test
    void pageRequestUsesDefaultSizeForInvalidSize() {
        Pageable pageable = PaginationUtils.pageRequest(2, 0, 20);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
    }

    @Test
    void pageRequestSupportsCustomCeilings() {
        Pageable pageable = PaginationUtils.pageRequest(1, 500, 50, 200, Sort.unsorted());

        assertEquals(1, pageable.getPageNumber());
        assertEquals(200, pageable.getPageSize());
    }
}
