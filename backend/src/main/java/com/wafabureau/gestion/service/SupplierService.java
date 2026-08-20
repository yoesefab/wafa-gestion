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

import org.springframework.stereotype.Service;

import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.supplier.SupplierResponse;

@Service
public class SupplierService extends PartnerService<Supplier, SupplierResponse> {

    public SupplierService(SupplierRepository repository) {
        super(repository, "Supplier");
    }

    @Override
    protected Supplier newEntity(PartnerWriteRequest request) {
        return new Supplier(
                request.name(),
                request.ice(),
                request.contactPerson(),
                request.email(),
                request.phone(),
                request.address()
        );
    }

    @Override
    protected SupplierResponse toResponse(Supplier supplier) {
        return SupplierMapper.toResponse(supplier);
    }
}
