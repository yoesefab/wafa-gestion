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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanCatalog() {
        jdbcTemplate.update("DELETE FROM stock_movements");
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void categoryCanBeCreatedUpdatedListedAndArchived() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" Furniture \",\"description\":\"Office furniture\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Furniture"));

        Category category = categoryRepository.findAll().getFirst();
        mockMvc.perform(put("/api/categories/{id}", category.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Office Furniture","description":null,"version":%d}
                                """.formatted(category.getVersion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Office Furniture"));

        Category updated = categoryRepository.findById(category.getId()).orElseThrow();
        mockMvc.perform(post("/api/categories/{id}/archive", updated.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + updated.getVersion() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/categories").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void archivedCategoryCannotBeAssignedToANewProduct() throws Exception {
        Category category = categoryRepository.saveAndFlush(new Category("Archived", null));
        category.archive();
        categoryRepository.saveAndFlush(category);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"TEST-1","name":"Test","categoryId":%d,
                                  "unitOfMeasure":"PIECE","purchasePrice":1.00,
                                  "sellingPrice":2.00,"minimumStock":0
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INACTIVE_REFERENCE"));
    }
}
