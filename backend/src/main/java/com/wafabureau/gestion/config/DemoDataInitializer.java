package com.wafabureau.gestion.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.dto.category.CategoryWriteRequest;
import com.wafabureau.gestion.dto.partner.PartnerWriteRequest;
import com.wafabureau.gestion.dto.product.ProductResponse;
import com.wafabureau.gestion.dto.product.ProductWriteRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderDetailResponse;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderLineWriteRequest;
import com.wafabureau.gestion.dto.purchase.PurchaseOrderWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderDetailResponse;
import com.wafabureau.gestion.dto.sales.SalesOrderLineWriteRequest;
import com.wafabureau.gestion.dto.sales.SalesOrderWriteRequest;
import com.wafabureau.gestion.enums.AdjustmentDirection;
import com.wafabureau.gestion.enums.UnitOfMeasure;
import com.wafabureau.gestion.model.User;
import com.wafabureau.gestion.repository.UserRepository;
import com.wafabureau.gestion.service.CategoryService;
import com.wafabureau.gestion.service.CustomerService;
import com.wafabureau.gestion.service.InventoryService;
import com.wafabureau.gestion.service.ProductService;
import com.wafabureau.gestion.service.PurchaseOrderService;
import com.wafabureau.gestion.service.SalesOrderService;
import com.wafabureau.gestion.service.SupplierService;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    public static final String DEMO_EMAIL = "admin@wafabureau.ma";
    public static final String DEMO_PASSWORD = "Admin123!";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Casablanca");
    private static final BigDecimal TAX_RATE = new BigDecimal("20.00");
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final List<String> CATEGORIES = List.of(
            "Papeterie",
            "Écriture et correction",
            "Classement et archivage",
            "Impression et consommables",
            "Mobilier de bureau",
            "Machines de bureau",
            "Informatique et périphériques",
            "Expédition et emballage"
    );

    private static final List<ProductSeed> PRODUCTS = List.of(
            product("PAP-A4-80", "Ramette Papier A4 80g", 0, UnitOfMeasure.PACK, "42.00", "55.00", 30),
            product("PAP-A3-80", "Ramette Papier A3 80g", 0, UnitOfMeasure.PACK, "85.00", "110.00", 10),
            product("BLOC-A5", "Bloc-notes A5 ligné", 0, UnitOfMeasure.PIECE, "12.00", "18.00", 20),
            product("POSTIT-76", "Notes adhésives 76 x 76 mm", 0, UnitOfMeasure.PACK, "9.00", "15.00", 25),
            product("REG-200", "Registre 200 pages", 0, UnitOfMeasure.PIECE, "28.00", "42.00", 8),
            product("BIC-BLEU", "Stylo BIC Bleu", 1, UnitOfMeasure.PIECE, "2.10", "3.50", 50),
            product("BIC-NOIR", "Stylo BIC Noir", 1, UnitOfMeasure.PIECE, "2.10", "3.50", 50),
            product("BIC-ROUGE", "Stylo BIC Rouge", 1, UnitOfMeasure.PIECE, "2.10", "3.50", 30),
            product("STY-GEL", "Stylo gel bleu 0,7 mm", 1, UnitOfMeasure.PIECE, "5.50", "9.00", 20),
            product("MARQ-PERM", "Marqueur permanent", 1, UnitOfMeasure.PIECE, "7.00", "12.00", 18),
            product("MARQ-TAB", "Marqueur tableau blanc", 1, UnitOfMeasure.PIECE, "8.00", "13.00", 18),
            product("CRAY-HB", "Crayon graphite HB", 1, UnitOfMeasure.PIECE, "1.80", "3.00", 35),
            product("GOM-BLC", "Gomme blanche", 1, UnitOfMeasure.PIECE, "2.20", "4.00", 20),
            product("CORR-RUB", "Ruban correcteur", 1, UnitOfMeasure.PIECE, "9.00", "15.00", 15),
            product("CLASS-A4", "Classeur A4 dos 8 cm", 2, UnitOfMeasure.PIECE, "24.00", "35.00", 20),
            product("CHEM-CART", "Chemise cartonnée", 2, UnitOfMeasure.PIECE, "2.80", "5.00", 40),
            product("POCH-PLAST", "Pochettes plastiques A4 - paquet de 100", 2, UnitOfMeasure.PACK, "18.00", "28.00", 20),
            product("BOX-ARCH", "Boîte d'archives dos 10 cm", 2, UnitOfMeasure.PIECE, "12.00", "20.00", 15),
            product("INTER-A4", "Intercalaires A4 12 positions", 2, UnitOfMeasure.PACK, "14.00", "22.00", 12),
            product("HP-305A-N", "Toner HP 305A Noir", 3, UnitOfMeasure.PIECE, "620.00", "790.00", 4),
            product("HP-305A-C", "Toner HP 305A Cyan", 3, UnitOfMeasure.PIECE, "690.00", "870.00", 3),
            product("CANON-057", "Toner Canon 057", 3, UnitOfMeasure.PIECE, "780.00", "990.00", 3),
            product("EPSON-103-N", "Bouteille d'encre Epson 103 Noir", 3, UnitOfMeasure.PIECE, "85.00", "120.00", 8),
            product("ROUL-THERM", "Rouleaux papier thermique 80 mm", 3, UnitOfMeasure.BOX, "95.00", "140.00", 6),
            product("CHAIR-ERGO", "Chaise de bureau ergonomique", 4, UnitOfMeasure.PIECE, "1150.00", "1550.00", 3),
            product("BUREAU-160", "Bureau professionnel 160 cm", 4, UnitOfMeasure.PIECE, "1800.00", "2400.00", 2),
            product("CHAIR-VIS", "Chaise visiteur", 4, UnitOfMeasure.PIECE, "420.00", "590.00", 4),
            product("CAIS-3T", "Caisson mobile 3 tiroirs", 4, UnitOfMeasure.PIECE, "650.00", "890.00", 2),
            product("ARMOIRE-2P", "Armoire de bureau 2 portes", 4, UnitOfMeasure.PIECE, "980.00", "1350.00", 1),
            product("AGRAF-MAP", "Agrafeuse Maped", 5, UnitOfMeasure.PIECE, "55.00", "82.00", 10),
            product("CALC-CASIO", "Calculatrice Casio", 5, UnitOfMeasure.PIECE, "130.00", "185.00", 8),
            product("DESTR-12", "Destructeur de documents", 5, UnitOfMeasure.PIECE, "950.00", "1290.00", 2),
            product("IMPR-HP-LJ", "Imprimante HP LaserJet", 5, UnitOfMeasure.PIECE, "2200.00", "2890.00", 2),
            product("PLAST-A4", "Plastifieuse A4", 5, UnitOfMeasure.PIECE, "480.00", "690.00", 2),
            product("PERFO-2T", "Perforatrice 2 trous", 5, UnitOfMeasure.PIECE, "65.00", "95.00", 8),
            product("CLAV-USB", "Clavier USB AZERTY", 6, UnitOfMeasure.PIECE, "95.00", "145.00", 8),
            product("SOUR-OPT", "Souris optique USB", 6, UnitOfMeasure.PIECE, "70.00", "110.00", 10),
            product("USB-32", "Clé USB 32 Go", 6, UnitOfMeasure.PIECE, "55.00", "85.00", 12),
            product("MULTI-6", "Multiprise 6 prises", 6, UnitOfMeasure.PIECE, "75.00", "115.00", 8),
            product("HDMI-2M", "Câble HDMI 2 m", 6, UnitOfMeasure.PIECE, "45.00", "75.00", 8),
            product("WEBCAM-FHD", "Webcam Full HD", 6, UnitOfMeasure.PIECE, "260.00", "390.00", 4),
            product("ENV-C4", "Enveloppe C4", 7, UnitOfMeasure.PIECE, "1.20", "2.50", 100),
            product("ENV-C5", "Enveloppe C5", 7, UnitOfMeasure.PIECE, "0.80", "1.80", 100),
            product("ADH-48", "Ruban adhésif 48 mm", 7, UnitOfMeasure.PIECE, "14.00", "24.00", 25),
            product("BULLE-10", "Rouleau papier bulle 10 m", 7, UnitOfMeasure.PIECE, "70.00", "110.00", 6)
    );

    private static final List<PartnerSeed> CUSTOMERS = List.of(
            partner("Atlas Conseil SARL", "001529874000031", "Nadia El Amrani", "achats@atlasconseil.ma", "0522 44 18 20", "12 boulevard Zerktouni, Casablanca"),
            partner("Cabinet Al Manar", "002174598000046", "Youssef Tazi", "contact@almanar.ma", "0537 70 22 14", "18 avenue Mohammed V, Rabat"),
            partner("Riad Services", "001963214000058", "Salma Bennis", "commande@riadservices.ma", "0524 38 11 90", "Quartier Guéliz, Marrakech"),
            partner("École Horizon", "002350147000022", "Amine Alaoui", "economat@horizon.ma", "0522 90 15 42", "Route d'El Jadida, Casablanca"),
            partner("Clinique Al Amal", "001741025000064", "Imane Chraïbi", "logistique@alamal.ma", "0535 62 08 33", "Avenue des FAR, Fès"),
            partner("Société Maghreb BTP", "002681930000019", "Karim Idrissi", "admin@maghrebbtp.ma", "0523 31 45 80", "Zone industrielle, Mohammedia"),
            partner("Fiduciaire Centrale", "001408752000071", "Sara Lahlou", "bureau@fiduciairecentrale.ma", "0522 27 60 10", "Maarif, Casablanca"),
            partner("Groupe Scolaire Ibn Rochd", "002057346000037", "Omar Bennani", "achats@ibnrochd.ma", "0539 94 12 08", "Centre-ville, Tanger"),
            partner("Translog Maroc", "001852479000053", "Mehdi Fassi", "services@translog.ma", "0522 35 70 90", "Aïn Sebaâ, Casablanca"),
            partner("Agence Pixel", null, "Lina Skalli", "hello@pixel.ma", "0537 68 31 24", "Agdal, Rabat"),
            partner("Coopérative Tissir", null, "Hajar Amrani", "contact@tissir.ma", "0528 82 16 40", "Talborjt, Agadir"),
            partner("Notaires Associés", "002749106000028", "Rachid Kettani", "secretariat@notairesassocies.ma", "0522 48 92 11", "Anfa, Casablanca")
    );

    private static final List<PartnerSeed> SUPPLIERS = List.of(
            partner("Disway Maroc", "000083720000078", "Service commercial", "ventes@disway.com", "0522 54 65 00", "Lotissement La Colline, Casablanca"),
            partner("HP Distribution Maroc", "001120480000035", "Compte revendeur", "channel@hp-maroc.ma", "0522 97 40 00", "Casablanca Nearshore"),
            partner("Canon Maroc", "000218540000066", "Service partenaires", "commandes@canon.ma", "0522 59 93 00", "Sidi Maârouf, Casablanca"),
            partner("Burostock", "001605479000043", "Mourad El Fassi", "commercial@burostock.ma", "0522 40 28 10", "Lissasfa, Casablanca"),
            partner("Papeterie du Maroc", "000341265000052", "Khadija Rami", "ventes@papeteriedumaroc.ma", "0537 76 40 22", "Yacoub El Mansour, Rabat"),
            partner("Mobilier Pro", "002014586000039", "Adil Mansouri", "devis@mobilierpro.ma", "0522 33 71 80", "Bouskoura"),
            partner("Maped Maroc Distribution", "001928476000017", "Service commandes", "commandes@maped-distribution.ma", "0522 66 91 30", "Aïn Harrouda"),
            partner("Emballage Express", "002460187000025", "Soukaina Tazi", "contact@emballageexpress.ma", "0523 32 18 72", "Zone industrielle, Berrechid")
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;
    private final InventoryService inventoryService;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CategoryService categoryService,
            ProductService productService,
            CustomerService customerService,
            SupplierService supplierService,
            PurchaseOrderService purchaseOrderService,
            SalesOrderService salesOrderService,
            InventoryService inventoryService,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryService = categoryService;
        this.productService = productService;
        this.customerService = customerService;
        this.supplierService = supplierService;
        this.purchaseOrderService = purchaseOrderService;
        this.salesOrderService = salesOrderService;
        this.inventoryService = inventoryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (userRepository.findByEmailIgnoreCase(DEMO_EMAIL).isPresent()) {
            log.info("Demo data already exists; skipping seed.");
            return;
        }

        User admin = userRepository.saveAndFlush(new User(
                "Admin", "WAFA", DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), true));
        Map<String, Long> categoryIds = seedCategories();
        Map<String, ProductSeed> productDefinitions = new LinkedHashMap<>();
        Map<String, Long> productIds = seedProducts(categoryIds, productDefinitions);
        List<Long> customerIds = seedPartners(CUSTOMERS, true);
        List<Long> supplierIds = seedPartners(SUPPLIERS, false);

        seedPurchases(admin.getId(), supplierIds, productIds, productDefinitions);
        seedSales(admin.getId(), customerIds, productIds);
        seedLowStock(admin.getId(), productIds);

        log.info("Demo data seeded. Local login: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private Map<String, Long> seedCategories() {
        Map<String, Long> result = new LinkedHashMap<>();
        CATEGORIES.forEach(name -> result.put(name,
                categoryService.create(new CategoryWriteRequest(name, "Données de démonstration")).id()));
        return result;
    }

    private Map<String, Long> seedProducts(
            Map<String, Long> categoryIds, Map<String, ProductSeed> definitions) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ProductSeed product : PRODUCTS) {
            ProductResponse created = productService.create(new ProductWriteRequest(
                    product.sku(), product.name(), categoryIds.get(CATEGORIES.get(product.categoryIndex())),
                    product.unit(), product.purchasePrice(), product.sellingPrice(), product.minimumStock()));
            result.put(product.sku(), created.id());
            definitions.put(product.sku(), product);
        }
        return result;
    }

    private List<Long> seedPartners(List<PartnerSeed> partners, boolean customers) {
        List<Long> ids = new ArrayList<>();
        for (PartnerSeed partner : partners) {
            PartnerWriteRequest request = new PartnerWriteRequest(
                    partner.name(), partner.ice(), partner.contact(), partner.email(), partner.phone(), partner.address());
            ids.add(customers ? customerService.create(request).id() : supplierService.create(request).id());
        }
        return ids;
    }

    private void seedPurchases(
            Long actorId,
            List<Long> supplierIds,
            Map<String, Long> productIds,
            Map<String, ProductSeed> definitions
    ) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        for (int categoryIndex = 0; categoryIndex < CATEGORIES.size(); categoryIndex++) {
            final int selectedCategory = categoryIndex;
            List<PurchaseOrderLineWriteRequest> lines = definitions.values().stream()
                    .filter(product -> product.categoryIndex() == selectedCategory)
                    .map(product -> new PurchaseOrderLineWriteRequest(
                            productIds.get(product.sku()),
                            Math.max(12L, product.minimumStock() * 4 + selectedCategory * 3L),
                            product.purchasePrice(), TAX_RATE))
                    .toList();
            LocalDate orderDate = today.minusDays(150L - categoryIndex * 12L);
            PurchaseOrderDetailResponse order = purchaseOrderService.create(new PurchaseOrderWriteRequest(
                    supplierIds.get(categoryIndex % supplierIds.size()), orderDate,
                    "Réapprovisionnement de démonstration", lines), actorId);
            purchaseOrderService.markOrdered(order.id());
            purchaseOrderService.receive(order.id(), actorId);
            backdatePurchase(order.id(), orderDate);
        }

        PurchaseOrderDetailResponse ordered = purchaseOrderService.create(new PurchaseOrderWriteRequest(
                supplierIds.getFirst(), today.minusDays(3), "Commande en attente de réception",
                List.of(purchaseLine("HP-305A-N", 8, productIds, definitions),
                        purchaseLine("CANON-057", 6, productIds, definitions))), actorId);
        purchaseOrderService.markOrdered(ordered.id());

        purchaseOrderService.create(new PurchaseOrderWriteRequest(
                supplierIds.get(4), today, "Brouillon à valider",
                List.of(purchaseLine("PAP-A4-80", 100, productIds, definitions),
                        purchaseLine("BIC-BLEU", 200, productIds, definitions))), actorId);

        PurchaseOrderDetailResponse cancelled = purchaseOrderService.create(new PurchaseOrderWriteRequest(
                supplierIds.get(5), today.minusDays(12), "Commande annulée après révision du devis",
                List.of(purchaseLine("BUREAU-160", 3, productIds, definitions))), actorId);
        purchaseOrderService.markOrdered(cancelled.id());
        purchaseOrderService.cancel(cancelled.id());
    }

    private PurchaseOrderLineWriteRequest purchaseLine(
            String sku, long quantity, Map<String, Long> ids, Map<String, ProductSeed> definitions) {
        ProductSeed product = definitions.get(sku);
        return new PurchaseOrderLineWriteRequest(ids.get(sku), quantity, product.purchasePrice(), TAX_RATE);
    }

    private void seedSales(Long actorId, List<Long> customerIds, Map<String, Long> productIds) {
        List<SaleSeed> completed = List.of(
                sale(330, lines(line("PAP-A4-80", 10), line("BIC-BLEU", 20), line("CHEM-CART", 30))),
                sale(270, lines(line("HP-305A-N", 1), line("PAP-A4-80", 5))),
                sale(210, lines(line("CHAIR-ERGO", 2), line("BUREAU-160", 1))),
                sale(150, lines(line("CANON-057", 1), line("ENV-C4", 50))),
                sale(110, lines(line("CALC-CASIO", 3), line("AGRAF-MAP", 5))),
                sale(75, lines(line("IMPR-HP-LJ", 1), line("HP-305A-N", 1))),
                sale(48, lines(line("MARQ-PERM", 12), line("MARQ-TAB", 6))),
                sale(36, lines(line("BOX-ARCH", 8), line("CLASS-A4", 10))),
                sale(29, lines(line("CLAV-USB", 3), line("SOUR-OPT", 3), line("USB-32", 5))),
                sale(22, lines(line("BLOC-A5", 12), line("POSTIT-76", 20))),
                sale(15, lines(line("DESTR-12", 1), line("PLAST-A4", 1))),
                sale(10, lines(line("ENV-C4", 80), line("ADH-48", 10))),
                sale(6, lines(line("PAP-A3-80", 5), line("REG-200", 6))),
                sale(2, lines(line("CHAIR-VIS", 4), line("CAIS-3T", 2)))
        );

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        for (int index = 0; index < completed.size(); index++) {
            SaleSeed seed = completed.get(index);
            LocalDate orderDate = today.minusDays(seed.daysAgo());
            SalesOrderDetailResponse order = salesOrderService.create(new SalesOrderWriteRequest(
                    customerIds.get(index % customerIds.size()), orderDate, "Vente de démonstration",
                    salesLines(seed.lines(), productIds)), actorId);
            salesOrderService.confirm(order.id(), actorId);
            boolean delivered = index % 3 != 0;
            if (delivered) salesOrderService.deliver(order.id());
            backdateSale(order.id(), orderDate, delivered);
        }

        for (int index = 0; index < 3; index++) {
            salesOrderService.create(new SalesOrderWriteRequest(
                    customerIds.get((index + 4) % customerIds.size()), today.minusDays(index),
                    "Brouillon commercial", salesLines(lines(
                            line(index == 0 ? "PAP-A4-80" : "BIC-NOIR", 4 + index)),
                            productIds)), actorId);
        }

        for (int index = 0; index < 2; index++) {
            SalesOrderDetailResponse order = salesOrderService.create(new SalesOrderWriteRequest(
                    customerIds.get((index + 8) % customerIds.size()), today.minusDays(5L + index),
                    "Commande client annulée", salesLines(lines(line("ENV-C5", 20 + index * 10L)), productIds)),
                    actorId);
            salesOrderService.cancel(order.id());
        }
    }

    private List<SalesOrderLineWriteRequest> salesLines(List<SaleLine> lines, Map<String, Long> productIds) {
        return lines.stream()
                .map(line -> new SalesOrderLineWriteRequest(
                        productIds.get(line.sku()), line.quantity(), null, TAX_RATE))
                .toList();
    }

    private void seedLowStock(Long actorId, Map<String, Long> productIds) {
        Map<String, Long> targets = Map.of(
                "PAP-A4-80", 25L,
                "BIC-BLEU", 40L,
                "CLASS-A4", 20L,
                "HP-305A-N", 3L,
                "CANON-057", 0L,
                "CHAIR-ERGO", 2L,
                "IMPR-HP-LJ", 1L,
                "ENV-C4", 70L
        );
        targets.forEach((sku, target) -> {
            ProductResponse product = productService.get(productIds.get(sku));
            long difference = product.currentStock() - target;
            if (difference == 0) return;
            AdjustmentDirection direction = difference > 0 ? AdjustmentDirection.OUT : AdjustmentDirection.IN;
            inventoryService.adjustStock(
                    product.id(), direction, Math.abs(difference),
                    "DEMO-COMP-" + LocalDate.now(BUSINESS_ZONE).getYear(),
                    "Comptage physique de démonstration", "Stock bas intentionnel", actorId);
        });
    }

    private void backdatePurchase(Long orderId, LocalDate date) {
        var timestamp = date.atTime(LocalTime.of(10, 0)).atZone(BUSINESS_ZONE).toOffsetDateTime();
        jdbcTemplate.update("UPDATE purchase_orders SET ordered_at = ?, received_at = ? WHERE id = ?",
                timestamp.minusDays(2), timestamp, orderId);
        jdbcTemplate.update("""
                UPDATE stock_movements SET occurred_at = ?
                WHERE purchase_order_item_id IN (
                    SELECT id FROM purchase_order_items WHERE purchase_order_id = ?
                )
                """, timestamp, orderId);
    }

    private void backdateSale(Long orderId, LocalDate date, boolean delivered) {
        var timestamp = date.atTime(LocalTime.of(14, 0)).atZone(BUSINESS_ZONE).toOffsetDateTime();
        jdbcTemplate.update("UPDATE sales_orders SET confirmed_at = ?, delivered_at = ? WHERE id = ?",
                timestamp, delivered ? timestamp.plusDays(1) : null, orderId);
        jdbcTemplate.update("""
                UPDATE stock_movements SET occurred_at = ?
                WHERE sales_order_item_id IN (
                    SELECT id FROM sales_order_items WHERE sales_order_id = ?
                )
                """, timestamp, orderId);
    }

    private static ProductSeed product(String sku, String name, int category, UnitOfMeasure unit,
                                       String purchasePrice, String sellingPrice, long minimumStock) {
        return new ProductSeed(sku, name, category, unit, new BigDecimal(purchasePrice),
                new BigDecimal(sellingPrice), minimumStock);
    }

    private static PartnerSeed partner(String name, String ice, String contact, String email,
                                       String phone, String address) {
        return new PartnerSeed(name, ice, contact, email, phone, address);
    }

    private static SaleLine line(String sku, long quantity) {
        return new SaleLine(sku, quantity);
    }

    private static List<SaleLine> lines(SaleLine... lines) {
        return List.of(lines);
    }

    private static SaleSeed sale(long daysAgo, List<SaleLine> lines) {
        return new SaleSeed(daysAgo, lines);
    }

    private record ProductSeed(String sku, String name, int categoryIndex, UnitOfMeasure unit,
                               BigDecimal purchasePrice, BigDecimal sellingPrice, long minimumStock) {
    }

    private record PartnerSeed(String name, String ice, String contact, String email, String phone, String address) {
    }

    private record SaleLine(String sku, long quantity) {
    }

    private record SaleSeed(long daysAgo, List<SaleLine> lines) {
    }
}
