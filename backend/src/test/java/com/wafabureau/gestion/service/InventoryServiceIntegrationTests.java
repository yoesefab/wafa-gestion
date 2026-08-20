package com.wafabureau.gestion.service;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.wafabureau.gestion.enums.UnitOfMeasure;
import com.wafabureau.gestion.service.CategoryService;
import com.wafabureau.gestion.service.ProductService;
import com.wafabureau.gestion.dto.category.CategoryResponse;
import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.product.ProductResponse;
import com.wafabureau.gestion.dto.product.ProductWriteRequest;
import com.wafabureau.gestion.dto.inventory.StockMovementResponse;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class InventoryServiceIntegrationTests {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MockMvc mockMvc;

    private Long productId;
    private Long actorId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM stock_movements");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM categories");
        userRepository.deleteAll();

        User actor = userRepository.saveAndFlush(new User(
                "Inventory",
                "Manager",
                "inventory@wafabureau.ma",
                passwordEncoder.encode("test-password"),
                true
        ));
        actorId = actor.getId();

        CategoryResponse category = categoryService.create(
                new CategoryWriteRequest("Office Supplies", null)
        );
        ProductResponse product = productService.create(new ProductWriteRequest(
                "PAP-A4",
                "A4 Paper",
                category.id(),
                UnitOfMeasure.PACK,
                new BigDecimal("42.50"),
                new BigDecimal("55.00"),
                10L
        ));
        productId = product.id();
    }

    @Test
    void increaseStockUpdatesBalanceAndCreatesMovement() {
        StockMovementResponse movement = inventoryService.increaseStock(
                productId, 25, "PO-OPENING", "Opening stock", null, actorId
        );

        assertThat(movement.movementType()).isEqualTo(StockMovementType.STOCK_IN);
        assertThat(movement.quantityDelta()).isEqualTo(25);
        assertThat(movement.stockBefore()).isZero();
        assertThat(movement.stockAfter()).isEqualTo(25);
        assertThat(movement.reference()).isEqualTo("PO-OPENING");
        assertThat(movement.reason()).isEqualTo("Opening stock");
        assertThat(movement.occurredAt()).isNotNull();
        assertThat(movement.createdBy().id()).isEqualTo(actorId);
        assertThat(productService.get(productId).currentStock()).isEqualTo(25);
        assertThat(stockMovementRepository.count()).isOne();
    }

    @Test
    void decreaseStockRecordsNegativeDelta() {
        inventoryService.increaseStock(productId, 10, "IN-1", "Receipt", null, actorId);

        StockMovementResponse movement = inventoryService.decreaseStock(
                productId, 4, "OUT-1", "Internal issue", "Warehouse A", actorId
        );

        assertThat(movement.movementType()).isEqualTo(StockMovementType.STOCK_OUT);
        assertThat(movement.quantityDelta()).isEqualTo(-4);
        assertThat(movement.stockBefore()).isEqualTo(10);
        assertThat(movement.stockAfter()).isEqualTo(6);
        assertThat(productService.get(productId).currentStock()).isEqualTo(6);
        assertThat(stockMovementRepository.count()).isEqualTo(2);
    }

    @Test
    void insufficientStockChangesNeitherBalanceNorLedger() {
        inventoryService.increaseStock(productId, 3, "IN-1", "Receipt", null, actorId);

        assertThatThrownBy(() -> inventoryService.decreaseStock(
                productId, 4, "OUT-1", "Issue", null, actorId
        ))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INSUFFICIENT_STOCK"));

        assertThat(productService.get(productId).currentStock()).isEqualTo(3);
        assertThat(stockMovementRepository.count()).isOne();
    }

    @Test
    void manualAdjustmentsAndRestoreUseExplicitStockMovementTypes() {
        StockMovementResponse inbound = inventoryService.adjustStock(
                productId,
                AdjustmentDirection.IN,
                8,
                "COUNT-1",
                "Physical count correction",
                null,
                actorId
        );
        StockMovementResponse outbound = inventoryService.adjustStock(
                productId,
                AdjustmentDirection.OUT,
                3,
                "COUNT-2",
                "Damaged stock",
                null,
                actorId
        );
        StockMovementResponse restored = inventoryService.restoreStock(
                productId, 2, "RESTORE-1", "Cancelled issue", null, actorId
        );

        assertThat(inbound.movementType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(inbound.quantityDelta()).isEqualTo(8);
        assertThat(outbound.movementType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(outbound.quantityDelta()).isEqualTo(-3);
        assertThat(restored.movementType()).isEqualTo(StockMovementType.RESTORE);
        assertThat(productService.get(productId).currentStock()).isEqualTo(7);
        assertThat(stockMovementRepository.count()).isEqualTo(3);
    }

    @Test
    void zeroAndNegativeQuantitiesAreRejectedBeforeAnyWrite() {
        assertThatThrownBy(() -> inventoryService.increaseStock(
                productId, 0, "IN-1", "Receipt", null, actorId
        )).isInstanceOf(RequestValidationException.class);

        assertThatThrownBy(() -> inventoryService.decreaseStock(
                productId, -1, "OUT-1", "Issue", null, actorId
        )).isInstanceOf(RequestValidationException.class);

        assertThatThrownBy(() -> inventoryService.adjustStock(
                productId, AdjustmentDirection.IN, 0, "ADJ-1", "Count", null, actorId
        )).isInstanceOf(RequestValidationException.class);

        assertThat(productService.get(productId).currentStock()).isZero();
        assertThat(stockMovementRepository.count()).isZero();
    }

    @Test
    void movementFailureRollsBackProductBalance() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            inventoryService.increaseStock(productId, 5, "IN-1", "Receipt", null, actorId);
            throw new IllegalStateException("failure in enclosing business operation");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(productService.get(productId).currentStock()).isZero();
        assertThat(stockMovementRepository.count()).isZero();
    }

    @Test
    void movementListingSupportsPaginationAndFilters() {
        inventoryService.increaseStock(productId, 10, "IN-1", "Receipt", null, actorId);
        inventoryService.decreaseStock(productId, 2, "OUT-1", "Issue", null, actorId);

        var page = inventoryService.list(
                0,
                1,
                "occurredAt,desc",
                productId,
                StockMovementType.STOCK_OUT,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertThat(page.data()).hasSize(1);
        assertThat(page.data().getFirst().movementType()).isEqualTo(StockMovementType.STOCK_OUT);
        assertThat(page.page().totalElements()).isOne();
    }

    @Test
    void concurrentDecreasesCannotSpendTheSameStock() throws Exception {
        inventoryService.increaseStock(productId, 10, "IN-1", "Receipt", null, actorId);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<StockMovementResponse>> futures = List.of(
                    executor.submit(() -> decreaseAfter(start, "OUT-A")),
                    executor.submit(() -> decreaseAfter(start, "OUT-B"))
            );
            start.countDown();

            int successes = 0;
            int insufficientFailures = 0;
            for (Future<StockMovementResponse> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    successes++;
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof BusinessException businessException
                            && "INSUFFICIENT_STOCK".equals(businessException.getCode())) {
                        insufficientFailures++;
                    } else {
                        throw exception;
                    }
                }
            }

            assertThat(successes).isOne();
            assertThat(insufficientFailures).isOne();
        }

        assertThat(productService.get(productId).currentStock()).isEqualTo(3);
        assertThat(stockMovementRepository.count()).isEqualTo(2);
    }

    @Test
    void authenticatedAdjustmentAndMovementListEndpointsUseTheDocumentedEnvelopes() throws Exception {
        String token = loginAndExtractToken();

        mockMvc.perform(post("/api/products/{id}/stock-adjustment", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "direction":"IN",
                                  "quantity":12,
                                  "reference":"COUNT-HTTP-1",
                                  "reason":"Opening physical count",
                                  "note":null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.movementType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.data.quantityDelta").value(12))
                .andExpect(jsonPath("$.data.stockBefore").value(0))
                .andExpect(jsonPath("$.data.stockAfter").value(12));

        mockMvc.perform(get("/api/stock-movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("productId", productId.toString())
                        .param("type", "ADJUSTMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].reference").value("COUNT-HTTP-1"));
    }

    private String loginAndExtractToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"inventory@wafabureau.ma","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private StockMovementResponse decreaseAfter(CountDownLatch start, String reference) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        return inventoryService.decreaseStock(productId, 7, reference, "Concurrent issue", null, actorId);
    }
}
