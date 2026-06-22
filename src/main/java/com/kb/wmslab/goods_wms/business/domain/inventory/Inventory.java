package com.kb.wmslab.goods_wms.business.domain.inventory;

import com.kb.wmslab.goods_wms.business.domain.common.exception.ExcessiveReservationReleaseException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InsufficientStockException;
import lombok.Getter;

@Getter
public class Inventory {

    private static final String INVALID_INCREASE_QUANTITY = "증가 수량은 0보다 커야 합니다.";
    private static final String INVALID_DECREASE_QUANTITY = "차감 수량은 0보다 커야 합니다.";
    private static final String INVALID_RESERVE_QUANTITY = "예약 수량은 0보다 커야 합니다.";
    private static final String INVALID_RELEASE_QUANTITY = "예약 해제 수량은 0보다 커야 합니다.";

    private final Long id;
    private final Long warehouseId;
    private final Long zoneId;
    private final Long productId;
    private int normalQuantity;
    private int damagedQuantity;
    private int pendingInspectionQuantity;
    private int reservedQuantity;

    private Inventory(Long id, Long warehouseId, Long zoneId, Long productId,
                      int normalQuantity, int damagedQuantity,
                      int pendingInspectionQuantity, int reservedQuantity) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.zoneId = zoneId;
        this.productId = productId;
        this.normalQuantity = normalQuantity;
        this.damagedQuantity = damagedQuantity;
        this.pendingInspectionQuantity = pendingInspectionQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public static Inventory create(Long warehouseId, Long zoneId, Long productId) {
        return new Inventory(null, warehouseId, zoneId, productId, 0, 0, 0, 0);
    }

    public static Inventory reconstitute(Long id, Long warehouseId, Long zoneId, Long productId,
                                          int normalQuantity, int damagedQuantity,
                                          int pendingInspectionQuantity, int reservedQuantity) {
        return new Inventory(id, warehouseId, zoneId, productId,
                normalQuantity, damagedQuantity, pendingInspectionQuantity, reservedQuantity);
    }

    public int getAvailableQuantity() {
        return normalQuantity - reservedQuantity;
    }

    public void increaseNormal(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException(INVALID_INCREASE_QUANTITY);
        this.normalQuantity += quantity;
    }

    public void increaseDamaged(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException(INVALID_INCREASE_QUANTITY);
        this.damagedQuantity += quantity;
    }

    public void increasePendingInspection(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException(INVALID_INCREASE_QUANTITY);
        this.pendingInspectionQuantity += quantity;
    }

    public void decreaseNormal(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException(INVALID_DECREASE_QUANTITY);
        int available = getAvailableQuantity();
        if (available < quantity) throw new InsufficientStockException(quantity, available);
        this.normalQuantity -= quantity;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException(INVALID_RESERVE_QUANTITY);
        if (getAvailableQuantity() < quantity) throw new InsufficientStockException(quantity, getAvailableQuantity());
        this.reservedQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException(INVALID_RELEASE_QUANTITY);
        if (this.reservedQuantity < quantity) throw new ExcessiveReservationReleaseException(quantity, this.reservedQuantity);
        this.reservedQuantity -= quantity;
    }
}
