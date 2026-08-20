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
public class CustomerSelector {

    private final CustomerRepository customerRepository;

    public CustomerSelector(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer require(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }
}
