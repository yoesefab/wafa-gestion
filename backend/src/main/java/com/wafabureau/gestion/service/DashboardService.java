package com.wafabureau.gestion.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wafabureau.gestion.dto.dashboard.DashboardProductReference;
import com.wafabureau.gestion.dto.dashboard.DashboardSalesResponse;
import com.wafabureau.gestion.dto.dashboard.DashboardSummaryResponse;
import com.wafabureau.gestion.dto.dashboard.LowStockProductResponse;
import com.wafabureau.gestion.dto.dashboard.MonthlySalesPoint;
import com.wafabureau.gestion.dto.dashboard.TopProductResponse;
import com.wafabureau.gestion.enums.SalesOrderStatus;
import com.wafabureau.gestion.exception.RequestValidationException;
import com.wafabureau.gestion.repository.ProductRepository;
import com.wafabureau.gestion.repository.SalesOrderRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Casablanca");
    // A sale contributes to revenue once it is confirmed; DELIVERED is the later state of that same sale.
    static final Set<SalesOrderStatus> REVENUE_STATUSES = Set.of(
            SalesOrderStatus.CONFIRMED, SalesOrderStatus.DELIVERED);
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    @Autowired
    public DashboardService(SalesOrderRepository salesOrderRepository, ProductRepository productRepository) {
        this(salesOrderRepository, productRepository, Clock.system(BUSINESS_ZONE));
    }

    DashboardService(SalesOrderRepository salesOrderRepository, ProductRepository productRepository, Clock clock) {
        this.salesOrderRepository = salesOrderRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public DashboardSummaryResponse summary() {
        YearMonth currentMonth = YearMonth.now(clock);
        DateRange range = monthRange(currentMonth);
        SalesOrderRepository.SalesAggregate sales = salesOrderRepository.aggregateSales(
                REVENUE_STATUSES, range.from(), range.to());
        return new DashboardSummaryResponse(
                money(sales.getRevenue()),
                value(sales.getOrderCount()),
                productRepository.countByActiveTrue(),
                productRepository.countActiveLowStock());
    }

    public DashboardSalesResponse sales(Integer requestedYear) {
        int year = requestedYear == null ? LocalDate.now(clock).getYear() : requestedYear;
        if (year < 2000 || year > 2100) {
            throw new RequestValidationException("year must be between 2000 and 2100.");
        }
        DateRange range = yearRange(year);
        Map<Integer, SalesOrderRepository.MonthlySalesAggregate> aggregates = salesOrderRepository
                .aggregateMonthlySales(range.from(), range.to()).stream()
                .collect(Collectors.toMap(SalesOrderRepository.MonthlySalesAggregate::getMonthNumber, value -> value));
        List<MonthlySalesPoint> months = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> monthlyPoint(year, month, aggregates.get(month)))
                .toList();
        return new DashboardSalesResponse(year, "MAD", months);
    }

    public List<TopProductResponse> topProducts(LocalDate dateFrom, LocalDate dateTo, int limit) {
        validateLimit(limit, 20);
        YearMonth currentMonth = YearMonth.now(clock);
        LocalDate from = dateFrom == null ? currentMonth.atDay(1) : dateFrom;
        LocalDate to = dateTo == null ? currentMonth.atEndOfMonth() : dateTo;
        if (from.isAfter(to)) {
            throw new RequestValidationException("dateFrom must be before or equal to dateTo.");
        }
        DateRange range = dateRange(from, to.plusDays(1));
        return salesOrderRepository.findTopProducts(REVENUE_STATUSES, range.from(), range.to(), PageRequest.of(0, limit))
                .stream()
                .map(row -> new TopProductResponse(
                        new DashboardProductReference(row.getProductId(), row.getSku(), row.getName()),
                        value(row.getQuantitySold()), money(row.getRevenue())))
                .toList();
    }

    public List<LowStockProductResponse> lowStock(int limit) {
        validateLimit(limit, 100);
        return productRepository.findActiveLowStock(PageRequest.of(0, limit)).stream()
                .map(product -> new LowStockProductResponse(
                        product.getId(), product.getSku(), product.getName(),
                        product.getCurrentStock(), product.getMinimumStock()))
                .toList();
    }

    private MonthlySalesPoint monthlyPoint(
            int year, int month, SalesOrderRepository.MonthlySalesAggregate aggregate) {
        String label = YearMonth.of(year, Month.of(month)).toString();
        return aggregate == null
                ? new MonthlySalesPoint(label, 0, ZERO)
                : new MonthlySalesPoint(label, value(aggregate.getOrderCount()), money(aggregate.getRevenue()));
    }

    private DateRange monthRange(YearMonth month) {
        return dateRange(month.atDay(1), month.plusMonths(1).atDay(1));
    }

    private DateRange yearRange(int year) {
        return dateRange(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
    }

    private DateRange dateRange(LocalDate from, LocalDate toExclusive) {
        return new DateRange(
                from.atStartOfDay(BUSINESS_ZONE).toInstant(),
                toExclusive.atStartOfDay(BUSINESS_ZONE).toInstant());
    }

    private void validateLimit(int limit, int maximum) {
        if (limit < 1 || limit > maximum) {
            throw new RequestValidationException("limit must be between 1 and " + maximum + ".");
        }
    }

    private static long value(Long value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2);
    }

    private record DateRange(Instant from, Instant to) {
    }
}
