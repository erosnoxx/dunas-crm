package com.crm.common.infrastructure.persistence.repositories;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Traduz paginação entre o tipo da application e o do Spring Data, nos dois sentidos.
 */
public class PageableAdapter {

    public static org.springframework.data.domain.Pageable toSpring(Pageable pageable) {
        if (pageable.sortBy() == null)
            return PageRequest.of(pageable.page(), pageable.size());

        var direction = "DESC".equalsIgnoreCase(pageable.sortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageable.page(), pageable.size(), direction, pageable.sortBy());
    }

    public static <D> Page<D> toDomain(org.springframework.data.domain.Page<D> page) {
        return new Page<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
