package com.wafabureau.gestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
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

import com.wafabureau.gestion.dto.category.CategoryResponse;
import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.common.ArchiveRequest;
import com.wafabureau.gestion.dto.dashboard.DashboardSalesResponse;
import com.wafabureau.gestion.dto.dashboard.DashboardSummaryResponse;
import com.wafabureau.gestion.dto.dashboard.LowStockProductResponse;
import com.wafabureau.gestion.dto.dashboard.TopProductResponse;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.product.ProductResponse;
import com.wafabureau.gestion.dto.product.ProductWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderDetailResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderLineWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderWriteRequest;
import com.wafabureau.gestion.enums.UnitOfMeasure;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DashboardServiceIntegrationTests {

    @Autowired private DashboardService dashboardService;
    @Autowired private SalesOrderService salesOrderService;
    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private CustomerService customerService;
    @Autowired private InventoryService inventoryService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private Long actorId;
    private Long customerId;
    private Long firstProductId;
    private Long secondProductId;
    private Long lowStockProductId;
    private Long firstConfirmedOrderId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        actorId = userRepository.saveAndFlush(new User(
                "Dashboard", "Tester", "dashboard@wafabureau.ma",
                passwordEncoder.encode("password"), true)).getId();
        customerId = customerService.create(new PartnerWriteRequest(
                "Dashboard Customer", null, null, null, null, null)).id();
        CategoryResponse category = categoryService.create(new CategoryWriteRequest("Dashboard category", null));

        firstProductId = createProduct(category.id(), "DASH-1", "First product", "10.00", 5);
        secondProductId = createProduct(category.id(), "DASH-2", "Second product", "7.50", 2);
        lowStockProductId = createProduct(category.id(), "DASH-3", "Urgent product", "5.00", 5);
        Long archivedProductId = createProduct(category.id(), "DASH-4", "Archived product", "4.00", 10);
        ProductResponse archived = productService.get(archivedProductId);
        productService.archive(archivedProductId, new ArchiveRequest(archived.version()));

        inventoryService.increaseStock(firstProductId, 20, "OPEN-1", "Opening stock", null, actorId);
        inventoryService.increaseStock(secondProductId, 20, "OPEN-2", "Opening stock", null, actorId);

        firstConfirmedOrderId = salesOrderService.confirm(
                createSale(List.of(line(firstProductId, 2))).id(), actorId).id();
        SalesOrderDetailResponse delivered = salesOrderService.confirm(createSale(List.of(
                line(firstProductId, 1), line(secondProductId, 4))).id(), actorId);
        salesOrderService.deliver(delivered.id());
        createSale(List.of(line(firstProductId, 8)));
        salesOrderService.cancel(createSale(List.of(line(secondProductId, 6))).id());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void summaryCountsOnlyConfirmedAndDeliveredSalesAndActiveProducts() {
        DashboardSummaryResponse summary = dashboardService.summary();

        assertThat(summary.revenueThisMonth()).isEqualByComparingTo("60.00");
        assertThat(summary.salesOrderCountThisMonth()).isEqualTo(2);
        assertThat(summary.totalActiveProducts()).isEqualTo(3);
        assertThat(summary.lowStockProductCount()).isEqualTo(1);
    }

    @Test
    void salesReturnsTwelveMonthlyPointsWithZeroFilledMonths() {
        int year = LocalDate.now(DashboardService.BUSINESS_ZONE).getYear();
        DashboardSalesResponse sales = dashboardService.sales(year);

        assertThat(sales.months()).hasSize(12);
        assertThat(sales.currency()).isEqualTo("MAD");
        assertThat(sales.months().get(YearMonth.now(DashboardService.BUSINESS_ZONE).getMonthValue() - 1).orderCount())
                .isEqualTo(2);
        assertThat(sales.months().get(YearMonth.now(DashboardService.BUSINESS_ZONE).getMonthValue() - 1).totalAmount())
                .isEqualByComparingTo("60.00");
        assertThat(sales.months().stream().map(point -> point.totalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("60.00");
    }

    @Test
    void salesGroupsConfirmationInstantsUsingCasablancaCalendarMonths() {
        jdbcTemplate.update("UPDATE sales_orders SET confirmed_at = ? WHERE id = ?",
                OffsetDateTime.parse("2025-12-31T23:30:00Z"), firstConfirmedOrderId);

        DashboardSalesResponse sales = dashboardService.sales(2026);

        assertThat(sales.months().getFirst().orderCount()).isEqualTo(1);
        assertThat(sales.months().getFirst().totalAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void topProductsUseSoldLineSnapshotsAndLowStockExcludesArchivedProducts() {
        List<TopProductResponse> topProducts = dashboardService.topProducts(null, null, 5);
        List<LowStockProductResponse> lowStock = dashboardService.lowStock(10);

        assertThat(topProducts).extracting(product -> product.product().id())
                .containsExactly(secondProductId, firstProductId);
        assertThat(topProducts).extracting(TopProductResponse::quantitySold).containsExactly(4L, 3L);
        assertThat(topProducts).extracting(TopProductResponse::revenue)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("30.00"), new BigDecimal("30.00"));
        assertThat(lowStock).extracting(LowStockProductResponse::id).containsExactly(lowStockProductId);
    }

    @Test
    void endpointsExposeAggregatesAndRejectInvalidRanges() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenueThisMonth").value(60.00))
                .andExpect(jsonPath("$.data.salesOrderCountThisMonth").value(2));
        mockMvc.perform(get("/api/dashboard/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.months.length()").value(12));
        mockMvc.perform(get("/api/dashboard/top-products").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/dashboard/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(lowStockProductId));

        assertThatThrownBy(() -> dashboardService.topProducts(
                LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1), 5))
                .isInstanceOf(RequestValidationException.class);
    }

    private Long createProduct(Long categoryId, String sku, String name, String price, long minimumStock) {
        return productService.create(new ProductWriteRequest(
                sku, name, categoryId, UnitOfMeasure.PIECE,
                new BigDecimal("2.00"), new BigDecimal(price), minimumStock)).id();
    }

    private SalesOrderDetailResponse createSale(List<SalesOrderLineWriteRequest> lines) {
        return salesOrderService.create(new SalesOrderWriteRequest(
                customerId, LocalDate.now(DashboardService.BUSINESS_ZONE), null, lines), actorId);
    }

    private SalesOrderLineWriteRequest line(Long productId, long quantity) {
        return new SalesOrderLineWriteRequest(productId, quantity, BigDecimal.ZERO, BigDecimal.ZERO);
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
