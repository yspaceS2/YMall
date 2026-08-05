package com.ymall.backend.admin.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
class AdminPageRequestFactory {

    private static final int MAX_PAGE_SIZE = 100;

    Pageable create(int page, int size) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
