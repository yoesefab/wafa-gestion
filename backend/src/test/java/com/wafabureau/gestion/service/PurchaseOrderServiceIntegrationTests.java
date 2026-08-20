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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.wafabureau.gestion.service.CategoryService;
import com.wafabureau.gestion.service.ProductService;
import com.wafabureau.gestion.enums.UnitOfMeasure;
import com.wafabureau.gestion.dto.category.CategoryResponse;
import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.product.ProductWriteRequest;
import com.wafabureau.gestion.service.SupplierService;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderDetailResponse;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderLineWriteRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderUpdateRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderWriteRequest;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PurchaseOrderServiceIntegrationTests {

    @Autowired private PurchaseOrderService purchaseService;
    @Autowired private SupplierService supplierService;
    @Autowired private CategoryService categoryService;
    @Autowired private ProductService productService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mockMvc;

    private Long actorId;
    private Long supplierId;
    private Long productId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        User user = userRepository.saveAndFlush(new User(
                "Purchase", "Manager", "purchase@wafabureau.ma", passwordEncoder.encode("password"), true));
        actorId = user.getId();
        supplierId = supplierService.create(new PartnerWriteRequest(
                "Maroc Office Supply", null, null, null, null, null)).id();
        CategoryResponse category = categoryService.create(new CategoryWriteRequest("Purchase products", null));
        productId = productService.create(new ProductWriteRequest(
                "PAP-A4", "A4 Paper", category.id(), UnitOfMeasure.PACK,
                new BigDecimal("40.00"), new BigDecimal("55.00"), 5L)).id();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createsDraftAndCalculatesHistoricalLineAndHeaderTotals() {
        PurchaseOrderDetailResponse order = createOrder(3, "42.50", "20.00");

        assertThat(order.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(order.orderNumber()).startsWith("PO-" + LocalDate.now().getYear());
        assertThat(order.supplier().id()).isEqualTo(supplierId);
        assertThat(order.lines()).hasSize(1);
        assertThat(order.lines().getFirst().unitPrice()).isEqualByComparingTo("42.50");
        assertThat(order.lines().getFirst().lineSubtotal()).isEqualByComparingTo("127.50");
        assertThat(order.lines().getFirst().lineTax()).isEqualByComparingTo("25.50");
        assertThat(order.subtotal()).isEqualByComparingTo("127.50");
        assertThat(order.taxAmount()).isEqualByComparingTo("25.50");
        assertThat(order.totalAmount()).isEqualByComparingTo("153.00");
    }

    @Test
    void draftCanBeUpdatedButOrderedOrderCannot() {
        PurchaseOrderDetailResponse draft = createOrder(1, "40.00", "0.00");
        PurchaseOrderDetailResponse updated = purchaseService.update(draft.id(), new PurchaseOrderUpdateRequest(
                supplierId, LocalDate.now(), "Updated", List.of(line(productId, 2, "41.00", "10.00")),
                draft.version()));
        assertThat(updated.lines().getFirst().quantity()).isEqualTo(2);
        assertThat(updated.totalAmount()).isEqualByComparingTo("90.20");

        PurchaseOrderDetailResponse ordered = purchaseService.markOrdered(updated.id());
        assertThatThrownBy(() -> purchaseService.update(ordered.id(), new PurchaseOrderUpdateRequest(
                supplierId, LocalDate.now(), null, List.of(line(productId, 1, "1.00", "0.00")),
                ordered.version())))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
    }

    @Test
    void markOrderedChangesStatusWithoutChangingStock() {
        PurchaseOrderDetailResponse draft = createOrder(4, "42.00", "0.00");

        PurchaseOrderDetailResponse ordered = purchaseService.markOrdered(draft.id());

        assertThat(ordered.status()).isEqualTo(PurchaseOrderStatus.ORDERED);
        assertThat(ordered.orderedAt()).isNotNull();
        assertThat(productService.get(productId).currentStock()).isZero();
        assertThat(countPurchaseMovements(draft.id())).isZero();
    }

    @Test
    void receiptIncreasesStockAndCreatesOneMovementPerLine() {
        Long secondProductId = createProduct("PEN-BLU", "Blue Pen");
        PurchaseOrderDetailResponse draft = purchaseService.create(new PurchaseOrderWriteRequest(
                supplierId,
                LocalDate.now(),
                "Multi-line receipt",
                List.of(
                        line(productId, 5, "42.00", "20.00"),
                        line(secondProductId, 7, "8.00", "10.00")
                )
        ), actorId);
        PurchaseOrderDetailResponse ordered = purchaseService.markOrdered(draft.id());

        PurchaseOrderDetailResponse received = purchaseService.receive(ordered.id(), actorId);

        assertThat(received.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(received.receivedAt()).isNotNull();
        assertThat(productService.get(productId).currentStock()).isEqualTo(5);
        assertThat(productService.get(secondProductId).currentStock()).isEqualTo(7);
        assertThat(countPurchaseMovements(ordered.id())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("""
                SELECT quantity_delta FROM stock_movements
                WHERE purchase_order_item_id IS NOT NULL
                ORDER BY quantity_delta
                """, Long.class)).containsExactly(5L, 7L);
    }

    @Test
    void duplicateReceiptIsRejectedWithoutDuplicateStockOrMovement() {
        PurchaseOrderDetailResponse ordered = purchaseService.markOrdered(
                createOrder(3, "42.00", "0.00").id());
        purchaseService.receive(ordered.id(), actorId);

        assertThatThrownBy(() -> purchaseService.receive(ordered.id(), actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        assertThat(productService.get(productId).currentStock()).isEqualTo(3);
        assertThat(countPurchaseMovements(ordered.id())).isOne();
    }

    @Test
    void invalidTransitionsReturnBusinessErrors() {
        PurchaseOrderDetailResponse draft = createOrder(1, "42.00", "0.00");
        assertThatThrownBy(() -> purchaseService.receive(draft.id(), actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));

        PurchaseOrderDetailResponse cancelled = purchaseService.cancel(draft.id());
        assertThatThrownBy(() -> purchaseService.receive(cancelled.id(), actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        assertThatThrownBy(() -> purchaseService.markOrdered(cancelled.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
    }

    @Test
    void draftAndOrderedOrdersCanBeCancelledButReceivedCannot() {
        assertThat(purchaseService.cancel(createOrder(1, "42.00", "0.00").id()).status())
                .isEqualTo(PurchaseOrderStatus.CANCELLED);
        PurchaseOrderDetailResponse ordered = purchaseService.markOrdered(createOrder(1, "42.00", "0.00").id());
        assertThat(purchaseService.cancel(ordered.id()).status()).isEqualTo(PurchaseOrderStatus.CANCELLED);

        PurchaseOrderDetailResponse received = purchaseService.receive(
                purchaseService.markOrdered(createOrder(1, "42.00", "0.00").id()).id(), actorId);
        assertThatThrownBy(() -> purchaseService.cancel(received.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
    }

    @Test
    void enclosingTransactionFailureRollsBackReceiptStockMovementAndStatus() {
        PurchaseOrderDetailResponse ordered = purchaseService.markOrdered(
                createOrder(6, "42.00", "0.00").id());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            purchaseService.receive(ordered.id(), actorId);
            throw new IllegalStateException("failure after receipt");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(productService.get(productId).currentStock()).isZero();
        assertThat(countPurchaseMovements(ordered.id())).isZero();
        assertThat(purchaseService.get(ordered.id()).status()).isEqualTo(PurchaseOrderStatus.ORDERED);
    }

    @Test
    void listAndDetailsSupportDocumentedQueries() throws Exception {
        PurchaseOrderDetailResponse order = createOrder(2, "42.00", "0.00");
        mockMvc.perform(get("/api/purchase-orders")
                        .param("search", order.orderNumber())
                        .param("supplierId", supplierId.toString())
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].orderNumber").value(order.orderNumber()));
        mockMvc.perform(get("/api/purchase-orders/{id}", order.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines.length()").value(1));
    }

    private PurchaseOrderDetailResponse createOrder(long quantity, String unitPrice, String taxRate) {
        return purchaseService.create(new PurchaseOrderWriteRequest(
                supplierId,
                LocalDate.now(),
                "Purchase test",
                List.of(line(productId, quantity, unitPrice, taxRate))
        ), actorId);
    }

    private PurchaseOrderLineWriteRequest line(
            Long targetProductId, long quantity, String unitPrice, String taxRate
    ) {
        return new PurchaseOrderLineWriteRequest(
                targetProductId, quantity, new BigDecimal(unitPrice), new BigDecimal(taxRate));
    }

    private Long createProduct(String sku, String name) {
        CategoryResponse category = categoryService.create(new CategoryWriteRequest("Category " + sku, null));
        return productService.create(new ProductWriteRequest(
                sku, name, category.id(), UnitOfMeasure.PIECE,
                new BigDecimal("8.00"), new BigDecimal("12.00"), 0L)).id();
    }

    private long countPurchaseMovements(Long orderId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM stock_movements sm
                JOIN purchase_order_items poi ON poi.id = sm.purchase_order_item_id
                WHERE poi.purchase_order_id = ?
                """, Long.class, orderId);
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM stock_movements");
        jdbcTemplate.update("DELETE FROM purchase_order_items");
        jdbcTemplate.update("DELETE FROM purchase_orders");
        jdbcTemplate.update("DELETE FROM sales_order_items");
        jdbcTemplate.update("DELETE FROM sales_orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM categories");
        jdbcTemplate.update("DELETE FROM customers");
        jdbcTemplate.update("DELETE FROM suppliers");
        jdbcTemplate.update("DELETE FROM app_users");
    }
}
