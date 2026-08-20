package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.sales.*;
import com.wafabureau.gestion.model.*;

public final class SalesOrderMapper {
    private SalesOrderMapper() { }

    public static SalesOrderSummaryResponse toSummary(SalesOrder order) {
        return new SalesOrderSummaryResponse(
                order.getId(), order.getOrderNumber(), toParty(order.getCustomer()), order.getOrderDate(),
                order.getStatus(), order.getSubtotal(), order.getDiscountAmount(), order.getTaxAmount(),
                order.getTotalAmount(), order.getVersion()
        );
    }

    public static SalesOrderDetailResponse toDetail(SalesOrder order) {
        return new SalesOrderDetailResponse(
                order.getId(), order.getOrderNumber(), toParty(order.getCustomer()), order.getOrderDate(),
                order.getStatus(), order.getSubtotal(), order.getDiscountAmount(), order.getTaxAmount(),
                order.getTotalAmount(), order.getNote(), order.getConfirmedAt(), order.getDeliveredAt(),
                toActor(order.getCreatedBy()), order.getCreatedAt(), order.getUpdatedAt(), order.getVersion(),
                order.getItems().stream().map(SalesOrderMapper::toItem).toList()
        );
    }

    private static SalesOrderItemResponse toItem(SalesOrderItem item) {
        return new SalesOrderItemResponse(
                item.getId(), toProduct(item.getProduct()), item.getQuantity(), item.getUnitPrice(),
                item.getTaxRate(), item.getLineSubtotal(), item.getLineTax(), item.getLineTotal()
        );
    }

    private static SalesPartyResponse toParty(Customer customer) {
        return new SalesPartyResponse(customer.getId(), customer.getName());
    }

    private static SalesProductResponse toProduct(Product product) {
        return new SalesProductResponse(product.getId(), product.getSku(), product.getName());
    }

    private static SalesActorResponse toActor(User user) {
        return new SalesActorResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
