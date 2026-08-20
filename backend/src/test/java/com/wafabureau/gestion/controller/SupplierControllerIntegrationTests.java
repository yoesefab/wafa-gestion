package com.wafabureau.gestion.controller;
import com.wafabureau.gestion.model.*;
import com.wafabureau.gestion.repository.*;
import com.wafabureau.gestion.service.*;
import com.wafabureau.gestion.enums.*;
import com.wafabureau.gestion.security.*;
import com.wafabureau.gestion.exception.*;
import com.wafabureau.gestion.dto.auth.*;
import com.wafabureau.gestion.dto.category.*;
import com.wafabureau.gestion.dto.product.*;
import com.wafabureau.gestion.dto.partner.*;
import com.wafabureau.gestion.dto.inventory.*;
import com.wafabureau.gestion.dto.sales.*;
import com.wafabureau.gestion.dto.purchase.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepository;

    @BeforeEach
    void cleanSuppliers() {
        supplierRepository.deleteAll();
    }

    @Test
    void supplierCrudAndValidationFollowThePartnerContract() throws Exception {
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Maroc Mobilier", "SUP-001", "contact@mobilier.ma")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Maroc Mobilier"))
                .andExpect(jsonPath("$.data.active").value(true));

        Supplier supplier = supplierRepository.findAll().getFirst();
        mockMvc.perform(get("/api/suppliers/{id}", supplier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("contact@mobilier.ma"));

        mockMvc.perform(put("/api/suppliers/{id}", supplier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Maroc Mobilier SARL", "SUP-001", "ventes@mobilier.ma", supplier.getVersion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Maroc Mobilier SARL"))
                .andExpect(jsonPath("$.data.email").value("ventes@mobilier.ma"));

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Invalid Supplier", null, "invalid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
    }

    @Test
    void supplierPaginationSearchAndArchivePreserveHistoricalAccess() throws Exception {
        Supplier mobilier = saveSupplier("Maroc Mobilier", "SUP-001", "mobilier@example.ma", "+212600001");
        saveSupplier("Papeterie Centrale", "SUP-002", "paper@example.ma", "+212600002");
        saveSupplier("Tech Office", "SUP-003", "tech@example.ma", "+212600003");

        mockMvc.perform(get("/api/suppliers")
                        .param("search", "SUP-002")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Papeterie Centrale"));

        mockMvc.perform(post("/api/suppliers/{id}/archive", mobilier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + mobilier.getVersion() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/suppliers").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/suppliers/{id}", mobilier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Maroc Mobilier"))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    private Supplier saveSupplier(String name, String ice, String email, String phone) {
        return supplierRepository.saveAndFlush(new Supplier(name, ice, null, email, phone, null));
    }

    private String body(String name, String ice, String email) {
        return """
                {
                  "name":"%s",
                  "ice":%s,
                  "contactPerson":null,
                  "email":"%s",
                  "phone":null,
                  "address":null
                }
                """.formatted(name, ice == null ? "null" : "\"" + ice + "\"", email);
    }

    private String updateBody(String name, String ice, String email, Long version) {
        return body(name, ice, email).replaceFirst("}\\s*$", ", \"version\": " + version + "}");
    }
}
