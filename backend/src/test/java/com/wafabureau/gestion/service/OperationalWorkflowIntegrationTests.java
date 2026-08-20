package com.wafabureau.gestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.wafabureau.gestion.dto.auth.LoginRequest;
import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.dashboard.DashboardSummaryResponse;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.product.ProductWriteRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderDetailResponse;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderLineWriteRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderDetailResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderLineWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderWriteRequest;
import com.wafabureau.gestion.enums.PurchaseOrderStatus;
import com.wafabureau.gestion.enums.SalesOrderStatus;
import com.wafabureau.gestion.enums.UnitOfMeasure;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OperationalWorkflowIntegrationTests {

    private static final String EMAIL = "workflow@wafabureau.ma";
    private static final String PASSWORD = "Admin123!";

    @Autowired private AuthService authService;
    @Autowired private CategoryService categoryService;
    @Autowired private CustomerService customerService;
    @Autowired private DashboardService dashboardService;
    @Autowired private InventoryService inventoryService;
    @Autowired private ProductService productService;
    @Autowired private PurchaseOrderService purchaseOrderService;
    @Autowired private SalesOrderService salesOrderService;
    @Autowired private SupplierService supplierService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private Long actorId;
    private Long customerId;
    private Long productId;
    private Long supplierId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        actorId = userRepository.saveAndFlush(new User(
                "Workflow", "Admin", EMAIL, passwordEncoder.encode(PASSWORD), true)).getId();
        customerId = customerService.create(new PartnerWriteRequest(
                "Atlas Professional Services", "001122334455667", "contact@atlas.ma",
                "+212522000000", "Casablanca", null)).id();
        supplierId = supplierService.create(new PartnerWriteRequest(
                "Maroc Office Distribution", "009988776655443", "orders@mod.ma",
                "+212522111111", "Casablanca", null)).id();
        Long categoryId = categoryService.create(new CategoryWriteRequest("Toners", null)).id();
        productId = productService.create(new ProductWriteRequest(
                "HP-305A-N", "Toner HP 305A Noir", categoryId, UnitOfMeasure.PIECE,
                new BigDecimal("620.00"), new BigDecimal("790.00"), 4L)).id();
        inventoryService.increaseStock(productId, 3, "OPENING-HP", "Opening demo stock", null, actorId);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void purchaseToSaleInvoiceAndDashboardWorkflowIsConsistent() throws Exception {
        assertThat(authService.login(new LoginRequest(EMAIL, PASSWORD)).accessToken()).isNotBlank();
        assertThat(dashboardService.lowStock(10)).extracting(product -> product.id()).contains(productId);
        assertThat(productService.get(productId).currentStock()).isEqualTo(3);

        PurchaseOrderDetailResponse draftPurchase = purchaseOrderService.create(new PurchaseOrderWriteRequest(
                supplierId, LocalDate.now(DashboardService.BUSINESS_ZONE), "Replenish low-stock toner",
                List.of(new PurchaseOrderLineWriteRequest(
                        productId, 20L, new BigDecimal("620.00"), BigDecimal.ZERO))), actorId);
        PurchaseOrderDetailResponse orderedPurchase = purchaseOrderService.markOrdered(draftPurchase.id());
        assertThat(orderedPurchase.status()).isEqualTo(PurchaseOrderStatus.ORDERED);
        assertThat(productService.get(productId).currentStock()).isEqualTo(3);

        PurchaseOrderDetailResponse receivedPurchase = purchaseOrderService.receive(orderedPurchase.id(), actorId);
        assertThat(receivedPurchase.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(productService.get(productId).currentStock()).isEqualTo(23);
        assertMovement(draftPurchase.orderNumber(), 20L, 3L, 23L, "Purchase order receipt");

        SalesOrderDetailResponse draftSale = salesOrderService.create(new SalesOrderWriteRequest(
                customerId, LocalDate.now(DashboardService.BUSINESS_ZONE), "Customer toner order",
                List.of(new SalesOrderLineWriteRequest(productId, 5L, null, BigDecimal.ZERO))), actorId);
        SalesOrderDetailResponse confirmedSale = salesOrderService.confirm(draftSale.id(), actorId);
        assertThat(confirmedSale.status()).isEqualTo(SalesOrderStatus.CONFIRMED);
        assertThat(salesOrderService.get(confirmedSale.id()).lines()).hasSize(1);
        assertThat(productService.get(productId).currentStock()).isEqualTo(18);
        assertMovement(draftSale.orderNumber(), -5L, 23L, 18L, "Sales order confirmation");

        byte[] invoice = mockMvc.perform(get("/api/sales-orders/{id}/invoice", confirmedSale.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(invoice).isNotEmpty();

        DashboardSummaryResponse summary = dashboardService.summary();
        assertThat(summary.revenueThisMonth()).isEqualByComparingTo("3950.00");
        assertThat(summary.salesOrderCountThisMonth()).isOne();
        assertThat(summary.totalActiveProducts()).isOne();
        assertThat(summary.lowStockProductCount()).isZero();
        assertThat(dashboardService.lowStock(10)).isEmpty();
        assertThat(dashboardService.topProducts(null, null, 5).getFirst().quantitySold()).isEqualTo(5);
    }

    private void assertMovement(String reference, long delta, long before, long after, String reason) {
        assertThat(jdbcTemplate.queryForList("""
                SELECT quantity_delta, stock_before, stock_after, reason
                FROM stock_movements
                WHERE reference = ?
                """, reference)).singleElement().satisfies(row -> {
            assertThat(((Number) row.get("quantity_delta")).longValue()).isEqualTo(delta);
            assertThat(((Number) row.get("stock_before")).longValue()).isEqualTo(before);
            assertThat(((Number) row.get("stock_after")).longValue()).isEqualTo(after);
            assertThat(row.get("reason")).isEqualTo(reason);
        });
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM stock_movements");
        jdbcTemplate.update("DELETE FROM sales_order_items");
        jdbcTemplate.update("DELETE FROM sales_orders");
        jdbcTemplate.update("DELETE FROM purchase_order_items");
        jdbcTemplate.update("DELETE FROM purchase_orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM categories");
        jdbcTemplate.update("DELETE FROM customers");
        jdbcTemplate.update("DELETE FROM suppliers");
        jdbcTemplate.update("DELETE FROM app_users");
    }
}
