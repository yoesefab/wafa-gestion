package com.wafabureau.gestion.mapper;

import com.wafabureau.gestion.dto.inventory.MovementActorResponse;
import com.wafabureau.gestion.dto.inventory.ProductReference;
import com.wafabureau.gestion.dto.inventory.StockMovementResponse;
import com.wafabureau.gestion.model.Product;
import com.wafabureau.gestion.model.StockMovement;
import com.wafabureau.gestion.model.User;

public final class StockMovementMapper {
    private StockMovementMapper() { }

    public static StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(), toProductReference(movement.getProduct()), movement.getStockMovementType(),
                movement.getQuantityDelta(), movement.getStockBefore(), movement.getStockAfter(),
                movement.getReference(), movement.getReason(), movement.getNote(),
                toActorResponse(movement.getCreatedBy()), movement.getOccurredAt()
        );
    }

    private static ProductReference toProductReference(Product product) {
        return new ProductReference(product.getId(), product.getSku(), product.getName());
    }

    private static MovementActorResponse toActorResponse(User user) {
        return new MovementActorResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
