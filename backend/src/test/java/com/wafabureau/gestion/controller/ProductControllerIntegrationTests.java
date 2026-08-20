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

import java.math.BigDecimal;

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
class ProductControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Category category;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM stock_movements");
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.saveAndFlush(new Category("Office Supplies", "Daily supplies"));
    }

    @Test
    void duplicateReferenceIsRejectedCaseInsensitively() throws Exception {
        createProduct("PAP-A4", "A4 Paper", category, 10);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(" pap-a4 ", "Other Paper", category.getId(), "20.00", "30.00", 5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void createProductStartsActiveWithZeroStock() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("PAP-A4", "A4 Paper", category.getId(), "42.50", "55.00", 10)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/products/\\d+")))
                .andExpect(jsonPath("$.data.sku").value("PAP-A4"))
                .andExpect(jsonPath("$.data.category.id").value(category.getId()))
                .andExpect(jsonPath("$.data.purchasePrice").value(42.50))
                .andExpect(jsonPath("$.data.currentStock").value(0))
                .andExpect(jsonPath("$.data.minimumStock").value(10))
                .andExpect(jsonPath("$.data.lowStock").value(true))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void updateChangesMasterDataButPreservesCurrentStock() throws Exception {
        Product product = createProduct("CHAIR-1", "Office Chair", category, 2);
        jdbcTemplate.update("UPDATE products SET current_stock = 7 WHERE id = ?", product.getId());

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(
                                "CHAIR-1",
                                "Ergonomic Office Chair",
                                category.getId(),
                                "700.00",
                                "950.00",
                                3,
                                product.getVersion()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Ergonomic Office Chair"))
                .andExpect(jsonPath("$.data.sellingPrice").value(950.00))
                .andExpect(jsonPath("$.data.minimumStock").value(3))
                .andExpect(jsonPath("$.data.currentStock").value(7));
    }

    @Test
    void listSupportsPaginationAndNameOrReferenceSearch() throws Exception {
        createProduct("PAP-A4", "A4 Paper", category, 10);
        createProduct("PAP-A3", "A3 Paper", category, 4);
        createProduct("PEN-BLU", "Blue Pen", category, 20);

        mockMvc.perform(get("/api/products")
                        .param("search", "paper")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/api/products").param("search", "pen-blu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Blue Pen"));
    }

    @Test
    void lowStockEndpointReturnsOnlyActiveProductsAtOrBelowThreshold() throws Exception {
        Product low = createProduct("PAP-A4", "A4 Paper", category, 10);
        Product healthy = createProduct("PEN-BLU", "Blue Pen", category, 2);
        Product archivedLow = createProduct("OLD-PEN", "Old Pen", category, 5);
        jdbcTemplate.update("UPDATE products SET current_stock = 8 WHERE id = ?", low.getId());
        jdbcTemplate.update("UPDATE products SET current_stock = 10 WHERE id = ?", healthy.getId());
        jdbcTemplate.update(
                "UPDATE products SET active = FALSE, deactivated_at = CURRENT_TIMESTAMP WHERE id = ?",
                archivedLow.getId()
        );

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].sku").value("PAP-A4"))
                .andExpect(jsonPath("$.data[0].lowStock").value(true));

        mockMvc.perform(get("/api/products").param("lowStock", "true").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void negativePriceIsRejectedByBeanValidation() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("PAP-A4", "A4 Paper", category.getId(), "-0.01", "55.00", 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'purchasePrice')]").exists());
    }

    @Test
    void archivePreservesProductAndExcludesItFromActiveLists() throws Exception {
        Product product = createProduct("PAP-A4", "A4 Paper", category, 10);

        mockMvc.perform(post("/api/products/{id}/archive", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + product.getVersion() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.deactivatedAt").isNotEmpty());

        mockMvc.perform(get("/api/products").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    private Product createProduct(String sku, String name, Category targetCategory, long minimumStock) {
        return productRepository.saveAndFlush(new Product(
                sku,
                name,
                targetCategory,
                UnitOfMeasure.PACK,
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                minimumStock
        ));
    }

    private String productBody(
            String sku,
            String name,
            Long categoryId,
            String purchasePrice,
            String sellingPrice,
            long minimumStock
    ) {
        return """
                {
                  "sku": "%s",
                  "name": "%s",
                  "categoryId": %d,
                  "unitOfMeasure": "PACK",
                  "purchasePrice": %s,
                  "sellingPrice": %s,
                  "minimumStock": %d
                }
                """.formatted(sku, name, categoryId, purchasePrice, sellingPrice, minimumStock);
    }

    private String updateBody(
            String sku,
            String name,
            Long categoryId,
            String purchasePrice,
            String sellingPrice,
            long minimumStock,
            Long version
    ) {
        return productBody(sku, name, categoryId, purchasePrice, sellingPrice, minimumStock)
                .replaceFirst("}\\s*$", ", \"version\": " + version + "}");
    }
}
