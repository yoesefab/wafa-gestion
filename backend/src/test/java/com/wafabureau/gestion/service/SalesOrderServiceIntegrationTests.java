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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.wafabureau.gestion.service.CategoryService;
import com.wafabureau.gestion.service.ProductService;
import com.wafabureau.gestion.enums.UnitOfMeasure;
import com.wafabureau.gestion.dto.category.CategoryResponse;
import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.product.ProductResponse;
import com.wafabureau.gestion.dto.product.ProductWriteRequest;
import com.wafabureau.gestion.service.InventoryService;
import com.wafabureau.gestion.service.CustomerService;
import com.wafabureau.gestion.dto.customer.CustomerResponse;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderDetailResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderLineWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderUpdateRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderWriteRequest;
import com.wafabureau.gestion.exception.BusinessException;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SalesOrderServiceIntegrationTests {

    @Autowired private SalesOrderService salesService;
    @Autowired private CategoryService categoryService;
    @Autowired private ProductService productService;
    @Autowired private CustomerService customerService;
    @Autowired private InventoryService inventoryService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private Long actorId;
    private Long customerId;
    private Long productId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        User user = userRepository.saveAndFlush(new User(
                "Sales", "Manager", "sales@wafabureau.ma", passwordEncoder.encode("password"), true));
        actorId = user.getId();
        customerId = customerService.create(new PartnerWriteRequest(
                "Atlas Services", null, null, null, null, null)).id();
        CategoryResponse category = categoryService.create(new CategoryWriteRequest("Office supplies", null));
        productId = productService.create(new ProductWriteRequest(
                "PAP-A4", "A4 Paper", category.id(), UnitOfMeasure.PACK,
                new BigDecimal("42.50"), new BigDecimal("55.00"), 5L)).id();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void createsDraftWithServerPriceAndCalculatedTotals() {
        SalesOrderDetailResponse order = createOrder(3, new BigDecimal("20.00"), new BigDecimal("0.01"));

        assertThat(order.status()).isEqualTo(SalesOrderStatus.DRAFT);
        assertThat(order.orderNumber()).startsWith("SO-" + LocalDate.now().getYear());
        assertThat(order.lines()).hasSize(1);
        assertThat(order.lines().getFirst().unitPrice()).isEqualByComparingTo("55.00");
        assertThat(order.lines().getFirst().lineSubtotal()).isEqualByComparingTo("165.00");
        assertThat(order.lines().getFirst().lineTax()).isEqualByComparingTo("33.00");
        assertThat(order.subtotal()).isEqualByComparingTo("165.00");
        assertThat(order.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(order.taxAmount()).isEqualByComparingTo("33.00");
        assertThat(order.totalAmount()).isEqualByComparingTo("198.00");
    }

    @Test
    void rejectsInvalidAndDuplicateItems() {
        assertThatThrownBy(() -> salesService.create(request(List.of(
                line(productId, 0, "20.00", "999.00"))), actorId))
                .isInstanceOf(RequestValidationException.class);

        assertThatThrownBy(() -> salesService.create(request(List.of(
                line(productId, 1, "20.00", "999.00"),
                line(productId, 2, "20.00", "1.00"))), actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("DUPLICATE_ORDER_ITEM"));
    }

    @Test
    void draftUpdateRepricesFromCurrentProductPriceAndChecksVersion() {
        SalesOrderDetailResponse order = createOrder(1, new BigDecimal("20.00"), BigDecimal.ZERO);
        ProductResponse product = productService.get(productId);
        productService.update(productId, new com.wafabureau.gestion.dto.product.ProductUpdateRequest(
                product.sku(), product.name(), product.category().id(), product.unitOfMeasure(),
                product.purchasePrice(), new BigDecimal("60.00"), product.minimumStock(), product.version()));

        SalesOrderDetailResponse updated = salesService.update(order.id(), new SalesOrderUpdateRequest(
                customerId, LocalDate.now(), "Updated", List.of(line(productId, 2, "0.00", "1.00")), order.version()));

        assertThat(updated.lines().getFirst().unitPrice()).isEqualByComparingTo("60.00");
        assertThat(updated.totalAmount()).isEqualByComparingTo("120.00");
        assertThatThrownBy(() -> salesService.update(order.id(), new SalesOrderUpdateRequest(
                customerId, LocalDate.now(), null, List.of(line(productId, 1, "0.00", "1.00")), order.version())))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("VERSION_CONFLICT"));
    }

    @Test
    void confirmationAtomicallyDecreasesStockAndCreatesOneMovementPerItem() {
        inventoryService.increaseStock(productId, 10, "OPEN", "Opening stock", null, actorId);
        SalesOrderDetailResponse draft = createOrder(4, new BigDecimal("20.00"), BigDecimal.ZERO);

        SalesOrderDetailResponse confirmed = salesService.confirm(draft.id(), actorId);

        assertThat(confirmed.status()).isEqualTo(SalesOrderStatus.CONFIRMED);
        assertThat(confirmed.confirmedAt()).isNotNull();
        assertThat(productService.get(productId).currentStock()).isEqualTo(6);
        assertThat(countSaleMovements(draft.id())).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT quantity_delta FROM stock_movements WHERE sales_order_item_id IS NOT NULL", Long.class))
                .isEqualTo(-4L);
    }

    @Test
    void movementSourceCannotReferenceAnOrderItemForAnotherProduct() {
        Long otherProductId = createProduct("PEN-BLU", "Blue Pen", "8.00");
        SalesOrderDetailResponse draft = createOrder(1, BigDecimal.ZERO, BigDecimal.ZERO);
        Long salesOrderItemId = draft.lines().getFirst().id();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO stock_movements (
                    product_id, movement_type, quantity_delta, stock_before, stock_after,
                    reference, reason, sales_order_item_id, created_by_user_id, occurred_at
                ) VALUES (?, 'STOCK_OUT', -1, 1, 0, 'INVALID', 'Invalid source', ?, ?, CURRENT_TIMESTAMP)
                """, otherProductId, salesOrderItemId, actorId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insufficientStockRollsBackAllLinesMovementsAndStatus() {
        Long secondProduct = createProduct("PEN-BLU", "Blue Pen", "8.00");
        inventoryService.increaseStock(productId, 10, "OPEN-1", "Opening stock", null, actorId);
        inventoryService.increaseStock(secondProduct, 1, "OPEN-2", "Opening stock", null, actorId);
        SalesOrderDetailResponse draft = salesService.create(request(List.of(
                line(productId, 4, "0.00", "1.00"),
                line(secondProduct, 2, "0.00", "1.00"))), actorId);

        assertThatThrownBy(() -> salesService.confirm(draft.id(), actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INSUFFICIENT_STOCK"));

        assertThat(productService.get(productId).currentStock()).isEqualTo(10);
        assertThat(productService.get(secondProduct).currentStock()).isEqualTo(1);
        assertThat(salesService.get(draft.id()).status()).isEqualTo(SalesOrderStatus.DRAFT);
        assertThat(countSaleMovements(draft.id())).isZero();
    }

    @Test
    void duplicateConfirmationIsRejectedWithoutDuplicateMovement() {
        inventoryService.increaseStock(productId, 10, "OPEN", "Opening stock", null, actorId);
        SalesOrderDetailResponse draft = createOrder(2, BigDecimal.ZERO, BigDecimal.ZERO);
        salesService.confirm(draft.id(), actorId);

        assertThatThrownBy(() -> salesService.confirm(draft.id(), actorId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        assertThat(productService.get(productId).currentStock()).isEqualTo(8);
        assertThat(countSaleMovements(draft.id())).isOne();
    }

    @Test
    void draftCancellationHasNoStockEffectAndConfirmedCancellationDoesNotRestoreStock() {
        inventoryService.increaseStock(productId, 10, "OPEN", "Opening stock", null, actorId);
        SalesOrderDetailResponse cancelled = salesService.cancel(createOrder(2, BigDecimal.ZERO, BigDecimal.ZERO).id());
        assertThat(cancelled.status()).isEqualTo(SalesOrderStatus.CANCELLED);
        assertThat(productService.get(productId).currentStock()).isEqualTo(10);

        SalesOrderDetailResponse confirmed = salesService.confirm(
                createOrder(3, BigDecimal.ZERO, BigDecimal.ZERO).id(), actorId);
        assertThatThrownBy(() -> salesService.cancel(confirmed.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
        assertThat(productService.get(productId).currentStock()).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_movements WHERE movement_type = 'RESTORE'", Long.class)).isZero();
    }

    @Test
    void deliveryRequiresConfirmationAndMakesOrderImmutable() {
        SalesOrderDetailResponse draft = createOrder(1, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThatThrownBy(() -> salesService.deliver(draft.id())).isInstanceOf(BusinessException.class);
        inventoryService.increaseStock(productId, 2, "OPEN", "Opening stock", null, actorId);
        SalesOrderDetailResponse delivered = salesService.deliver(salesService.confirm(draft.id(), actorId).id());

        assertThat(delivered.status()).isEqualTo(SalesOrderStatus.DELIVERED);
        assertThat(delivered.deliveredAt()).isNotNull();
        assertThatThrownBy(() -> salesService.update(delivered.id(), new SalesOrderUpdateRequest(
                customerId, LocalDate.now(), null, List.of(line(productId, 1, "0.00", "1.00")), delivered.version())))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_STATE_TRANSITION"));
    }

    @Test
    void listAndDetailsFollowApiFilters() throws Exception {
        SalesOrderDetailResponse order = createOrder(1, BigDecimal.ZERO, BigDecimal.ZERO);
        mockMvc.perform(get("/api/sales-orders").param("search", order.orderNumber()).param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].orderNumber").value(order.orderNumber()));
        mockMvc.perform(get("/api/sales-orders/{id}", order.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines.length()").value(1));
    }

    @Test
    void invoiceEndpointReturnsANonEmptyPdfForAConfirmedOrder() throws Exception {
        inventoryService.increaseStock(productId, 5, "OPEN", "Opening stock", null, actorId);
        SalesOrderDetailResponse confirmed = salesService.confirm(
                createOrder(2, new BigDecimal("20.00"), BigDecimal.ZERO).id(), actorId);
        ProductResponse currentProduct = productService.get(productId);
        productService.update(productId, new com.wafabureau.gestion.dto.product.ProductUpdateRequest(
                currentProduct.sku(), currentProduct.name(), currentProduct.category().id(),
                currentProduct.unitOfMeasure(), currentProduct.purchasePrice(), new BigDecimal("99.00"),
                currentProduct.minimumStock(), currentProduct.version()));

        byte[] pdf = mockMvc.perform(get("/api/sales-orders/{id}/invoice", confirmed.id()))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType())
                        .isEqualTo(MediaType.APPLICATION_PDF_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String invoiceText = new PDFTextStripper().getText(document);
            assertThat(invoiceText).contains("WAFA BUREAU", confirmed.orderNumber(), "55.00 MAD", "132.00 MAD");
            assertThat(invoiceText).doesNotContain("99.00 MAD");
        }
    }

    @Test
    void invoiceEndpointRejectsDraftOrders() throws Exception {
        SalesOrderDetailResponse draft = createOrder(1, BigDecimal.ZERO, BigDecimal.ZERO);

        mockMvc.perform(get("/api/sales-orders/{id}/invoice", draft.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_AVAILABLE"));
    }

    private SalesOrderDetailResponse createOrder(long quantity, BigDecimal taxRate, BigDecimal suppliedPrice) {
        return salesService.create(request(List.of(
                new SalesOrderLineWriteRequest(productId, quantity, suppliedPrice, taxRate))), actorId);
    }

    private SalesOrderWriteRequest request(List<SalesOrderLineWriteRequest> lines) {
        return new SalesOrderWriteRequest(customerId, LocalDate.now(), "Sales test", lines);
    }

    private SalesOrderLineWriteRequest line(Long targetProductId, long quantity, String tax, String suppliedPrice) {
        return new SalesOrderLineWriteRequest(
                targetProductId, quantity, new BigDecimal(suppliedPrice), new BigDecimal(tax));
    }

    private Long createProduct(String sku, String name, String price) {
        CategoryResponse category = categoryService.create(new CategoryWriteRequest("Category " + sku, null));
        return productService.create(new ProductWriteRequest(
                sku, name, category.id(), UnitOfMeasure.PIECE,
                new BigDecimal("5.00"), new BigDecimal(price), 0L)).id();
    }

    private long countSaleMovements(Long orderId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM stock_movements sm
                JOIN sales_order_items soi ON soi.id = sm.sales_order_item_id
                WHERE soi.sales_order_id = ?
                """, Long.class, orderId);
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM stock_movements");
        jdbcTemplate.update("DELETE FROM sales_order_items");
        jdbcTemplate.update("DELETE FROM sales_orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM categories");
        jdbcTemplate.update("DELETE FROM customers");
        jdbcTemplate.update("DELETE FROM suppliers");
        jdbcTemplate.update("DELETE FROM app_users");
    }
}
