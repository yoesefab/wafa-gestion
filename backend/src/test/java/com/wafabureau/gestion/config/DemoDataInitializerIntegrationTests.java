package com.wafabureau.gestion.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.wafabureau.gestion.repository.CategoryRepository;
import com.wafabureau.gestion.repository.CustomerRepository;
import com.wafabureau.gestion.repository.ProductRepository;
import com.wafabureau.gestion.repository.PurchaseOrderRepository;
import com.wafabureau.gestion.repository.SalesOrderRepository;
import com.wafabureau.gestion.repository.StockMovementRepository;
import com.wafabureau.gestion.repository.SupplierRepository;
import com.wafabureau.gestion.repository.UserRepository;
import com.wafabureau.gestion.service.ProductService;

@SpringBootTest(properties = "app.demo-data.enabled=true")
@ActiveProfiles({"test", "dev"})
@DirtiesContext
class DemoDataInitializerIntegrationTests {

    @Autowired private DemoDataInitializer initializer;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ProductService productService;

    @AfterEach
    void cleanDatabase() {
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

    @Test
    void seedsACompleteIdempotentDemoDataset() {
        assertThat(userRepository.count()).isOne();
        assertThat(categoryRepository.count()).isEqualTo(8);
        assertThat(productRepository.count()).isEqualTo(45);
        assertThat(customerRepository.count()).isEqualTo(12);
        assertThat(supplierRepository.count()).isEqualTo(8);
        assertThat(purchaseOrderRepository.count()).isEqualTo(11);
        assertThat(salesOrderRepository.count()).isEqualTo(19);
        assertThat(stockMovementRepository.count()).isGreaterThan(60);
        assertThat(productRepository.countActiveLowStock()).isEqualTo(8);
        Long hpTonerId = productRepository.findAll().stream()
                .filter(product -> product.getSku().equals("HP-305A-N"))
                .findFirst().orElseThrow().getId();
        assertThat(productService.get(hpTonerId)
                .currentStock()).isEqualTo(3);
        assertThat(passwordEncoder.matches(
                DemoDataInitializer.DEMO_PASSWORD,
                userRepository.findByEmailIgnoreCase(DemoDataInitializer.DEMO_EMAIL).orElseThrow().getPassword()))
                .isTrue();

        initializer.run(null);

        assertThat(userRepository.count()).isOne();
        assertThat(productRepository.count()).isEqualTo(45);
        assertThat(purchaseOrderRepository.count()).isEqualTo(11);
        assertThat(salesOrderRepository.count()).isEqualTo(19);
    }
}
