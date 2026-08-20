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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class CustomerControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanCustomers() {
        customerRepository.deleteAll();
    }

    @Test
    void customerCanBeCreatedReadAndUpdated() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerBody(
                                " Atlas Services SARL ",
                                "001234567000089",
                                "Salma Idrissi",
                                " CONTACT@ATLAS.EXAMPLE ",
                                "+212522000000",
                                "Casablanca, Morocco"
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/customers/\\d+")))
                .andExpect(jsonPath("$.data.name").value("Atlas Services SARL"))
                .andExpect(jsonPath("$.data.email").value("contact@atlas.example"))
                .andExpect(jsonPath("$.data.active").value(true));

        Customer customer = customerRepository.findAll().getFirst();
        mockMvc.perform(get("/api/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ice").value("001234567000089"));

        mockMvc.perform(put("/api/customers/{id}", customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                "Atlas Services",
                                null,
                                "Nadia Amrani",
                                "sales@atlas.example",
                                "+212522111111",
                                "Rabat, Morocco",
                                customer.getVersion()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Atlas Services"))
                .andExpect(jsonPath("$.data.ice").doesNotExist())
                .andExpect(jsonPath("$.data.contactPerson").value("Nadia Amrani"))
                .andExpect(jsonPath("$.data.address").value("Rabat, Morocco"));
    }

    @Test
    void customerValidationRejectsBlankNameAndInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerBody(" ", null, null, "not-an-email", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists());
    }

    @Test
    void customerListSupportsPaginationSearchAndArchiveVisibility() throws Exception {
        Customer atlas = saveCustomer("Atlas Services", "001", "atlas@example.ma", "+212500001");
        saveCustomer("Bureau Conseil", "002", "bureau@example.ma", "+212500002");
        saveCustomer("Casablanca Office", "003", "office@example.ma", "+212500003");

        mockMvc.perform(get("/api/customers")
                        .param("search", "atlas@example")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Atlas Services"));

        mockMvc.perform(post("/api/customers/{id}/archive", atlas.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + atlas.getVersion() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.deactivatedAt").isNotEmpty());

        mockMvc.perform(get("/api/customers").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/api/customers/{id}", atlas.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    private Customer saveCustomer(String name, String ice, String email, String phone) {
        return customerRepository.saveAndFlush(new Customer(name, ice, null, email, phone, null));
    }

    private String partnerBody(
            String name,
            String ice,
            String contactPerson,
            String email,
            String phone,
            String address
    ) {
        return """
                {
                  "name":%s,
                  "ice":%s,
                  "contactPerson":%s,
                  "email":%s,
                  "phone":%s,
                  "address":%s
                }
                """.formatted(
                json(name), json(ice), json(contactPerson), json(email), json(phone), json(address)
        );
    }

    private String updateBody(
            String name,
            String ice,
            String contactPerson,
            String email,
            String phone,
            String address,
            Long version
    ) {
        return partnerBody(name, ice, contactPerson, email, phone, address)
                .replaceFirst("}\\s*$", ", \"version\": " + version + "}");
    }

    private String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
