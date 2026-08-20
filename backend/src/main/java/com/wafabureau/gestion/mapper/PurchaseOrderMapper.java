package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.purchase.*;
import com.wafabureau.gestion.model.*;

public final class PurchaseOrderMapper {
    private PurchaseOrderMapper() { }

    public static PurchaseOrderSummaryResponse toSummary(PurchaseOrder order) {
        return new PurchaseOrderSummaryResponse(
                order.getId(), order.getOrderNumber(), toParty(order.getSupplier()), order.getOrderDate(),
                order.getStatus(), order.getSubtotal(), order.getDiscountAmount(), order.getTaxAmount(),
                order.getTotalAmount(), order.getVersion()
        );
    }

    public static PurchaseOrderDetailResponse toDetail(PurchaseOrder order) {
        return new PurchaseOrderDetailResponse(
                order.getId(), order.getOrderNumber(), toParty(order.getSupplier()), order.getOrderDate(),
                order.getStatus(), order.getSubtotal(), order.getDiscountAmount(), order.getTaxAmount(),
                order.getTotalAmount(), order.getNote(), order.getOrderedAt(), order.getReceivedAt(),
                toActor(order.getCreatedBy()), order.getCreatedAt(), order.getUpdatedAt(), order.getVersion(),
                order.getItems().stream().map(PurchaseOrderMapper::toItem).toList()
        );
    }

    private static PurchaseOrderItemResponse toItem(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(
                item.getId(), toProduct(item.getProduct()), item.getQuantity(), item.getUnitPrice(),
                item.getTaxRate(), item.getLineSubtotal(), item.getLineTax(), item.getLineTotal()
        );
    }

    private static PurchasePartyResponse toParty(Supplier supplier) {
        return new PurchasePartyResponse(supplier.getId(), supplier.getName());
    }

    private static PurchaseProductResponse toProduct(Product product) {
        return new PurchaseProductResponse(product.getId(), product.getSku(), product.getName());
    }

    private static PurchaseActorResponse toActor(User user) {
        return new PurchaseActorResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
