package com.abhay.dubairealestate.infrastructure.persistence.specification;

import com.abhay.dubairealestate.application.port.in.SalesFilter;
import com.abhay.dubairealestate.infrastructure.persistence.entity.SaleTransactionEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class SaleTransactionSpecification {

    private SaleTransactionSpecification() {}

    public static Specification<SaleTransactionEntity> withFilter(SalesFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.usage() != null && !filter.usage().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("usage")),
                        filter.usage().toLowerCase()
                ));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transValue"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transValue"), filter.maxPrice()));
            }
            if (filter.areaSearch() != null && !filter.areaSearch().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("areaName")),
                        "%" + filter.areaSearch().toLowerCase() + "%"
                ));
            }
            if (filter.projectSearch() != null && !filter.projectSearch().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("projectName")),
                        "%" + filter.projectSearch().toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
