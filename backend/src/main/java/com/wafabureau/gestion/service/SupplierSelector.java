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

import org.springframework.stereotype.Component;

import com.wafabureau.gestion.exception.ResourceNotFoundException;

@Component
public class SupplierSelector {

    private final SupplierRepository supplierRepository;

    public SupplierSelector(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public Supplier require(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
    }
}
