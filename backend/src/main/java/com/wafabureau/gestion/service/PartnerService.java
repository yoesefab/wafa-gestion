package com.wafabureau.gestion.service;
import com.wafabureau.gestion.mapper.*;
import com.wafabureau.gestion.exception.*;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.repository.*;
import com.wafabureau.gestion.repository.specification.*;
import com.wafabureau.gestion.dto.auth.*;
import com.wafabureau.gestion.dto.category.*;
import com.wafabureau.gestion.dto.product.*;
import com.wafabureau.gestion.dto.customer.*;
import com.wafabureau.gestion.dto.supplier.*;
import com.wafabureau.gestion.dto.partner.*;
import com.wafabureau.gestion.dto.inventory.*;
import com.wafabureau.gestion.dto.sales.*;
import com.wafabureau.gestion.dto.purchase.*;
import com.wafabureau.gestion.enums.*;
import com.wafabureau.gestion.security.*;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.dto.partner.PartnerUpdateRequest;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.common.ArchiveRequest;
import com.wafabureau.gestion.dto.common.PagedResponse;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.exception.ResourceNotFoundException;
import com.wafabureau.gestion.util.PageRequestFactory;

public abstract class PartnerService<T extends PartnerEntity, R> {

    private static final Set<String> SORT_FIELDS = Set.of("name", "createdAt");

    private final PartnerRepository<T> repository;
    private final String resourceName;

    protected PartnerService(PartnerRepository<T> repository, String resourceName) {
        this.repository = repository;
        this.resourceName = resourceName;
    }

    @Transactional
    public R create(PartnerWriteRequest request) {
        T partner = newEntity(request);
        return toResponse(repository.saveAndFlush(partner));
    }

    @Transactional(readOnly = true)
    public PagedResponse<R> list(
            int page,
            int size,
            String sort,
            String search,
            Boolean active
    ) {
        validateSearch(search);
        Page<T> partners = repository.findAll(
                PartnerSpecifications.matches(search, active),
                PageRequestFactory.create(page, size, sort, SORT_FIELDS)
        );
        return PageMapper.toResponse(partners, this::toResponse);
    }

    @Transactional(readOnly = true)
    public R get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public R update(Long id, PartnerUpdateRequest request) {
        T partner = find(id);
        verifyVersion(partner.getVersion(), request.version());
        partner.update(
                request.name(),
                request.ice(),
                request.contactPerson(),
                request.email(),
                request.phone(),
                request.address()
        );
        return toResponse(repository.saveAndFlush(partner));
    }

    @Transactional
    public R archive(Long id, ArchiveRequest request) {
        T partner = find(id);
        verifyVersion(partner.getVersion(), request.version());
        if (!partner.isActive()) {
            throw new BusinessException(
                    "INVALID_STATE_TRANSITION",
                    "The %s is already archived.".formatted(resourceName.toLowerCase())
            );
        }
        partner.archive();
        return toResponse(repository.saveAndFlush(partner));
    }

    protected abstract T newEntity(PartnerWriteRequest request);

    protected abstract R toResponse(T partner);

    private T find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(resourceName, id));
    }

    private void validateSearch(String search) {
        if (search != null && search.trim().length() > 254) {
            throw new RequestValidationException(
                    "%s search must not exceed 254 characters.".formatted(resourceName)
            );
        }
    }

    private void verifyVersion(Long actual, Long expected) {
        if (!actual.equals(expected)) {
            throw new BusinessException(
                    "VERSION_CONFLICT",
                    "The %s was modified by another request.".formatted(resourceName.toLowerCase())
            );
        }
    }
}
