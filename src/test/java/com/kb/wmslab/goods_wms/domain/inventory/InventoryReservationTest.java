package com.kb.wmslab.goods_wms.domain.inventory;

import com.kb.wmslab.goods_wms.business.domain.common.exception.ExcessiveReservationReleaseException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InsufficientStockException;
import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inventory 예약 (예약 / 해제 / 가용 재고 계산)")
class InventoryReservationTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.create(1L, 1L, 1L);
    }

    @Nested
    @DisplayName("재고 예약")
    class Reserve {

        @BeforeEach
        void setUp() {
            inventory.increaseNormal(100);
        }

        @Test
        @DisplayName("가용 재고 범위 내에서 예약하면 성공한다")
        void reservingWithinAvailableQuantityShouldSucceed() {
            inventory.reserve(60);
            assertThat(inventory.getReservedQuantity()).isEqualTo(60);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(40);
        }

        @Test
        @DisplayName("가용 재고 전체를 예약할 수 있다")
        void reservingEntireAvailableQuantityShouldSucceed() {
            inventory.reserve(100);
            assertThat(inventory.getReservedQuantity()).isEqualTo(100);
            assertThat(inventory.getAvailableQuantity()).isZero();
        }

        @Test
        @DisplayName("가용 재고를 초과하여 예약하면 InsufficientStockException이 발생한다")
        void reservingMoreThanAvailableQuantityShouldThrowException() {
            assertThatThrownBy(() -> inventory.reserve(101))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("이미 일부 예약된 상태에서 남은 가용 재고를 초과하면 예외가 발생한다")
        void reservingMoreThanRemainingAvailableQuantityShouldThrowException() {
            inventory.reserve(60);
            assertThatThrownBy(() -> inventory.reserve(50))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("0을 예약하면 예외가 발생한다")
        void zeroQuantityReserveShouldThrowException() {
            assertThatThrownBy(() -> inventory.reserve(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수를 예약하면 예외가 발생한다")
        void negativeQuantityReserveShouldThrowException() {
            assertThatThrownBy(() -> inventory.reserve(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("예약 후 일반 재고 수량은 변하지 않는다")
        void reservationShouldNotChangeNormalStockQuantity() {
            inventory.reserve(60);
            assertThat(inventory.getNormalQuantity()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("예약 해제")
    class ReleaseReservation {

        @BeforeEach
        void setUp() {
            inventory.increaseNormal(100);
            inventory.reserve(60);
        }

        @Test
        @DisplayName("예약 수량 이하로 해제하면 성공한다")
        void releasingWithinReservedQuantityShouldSucceed() {
            inventory.releaseReservation(60);
            assertThat(inventory.getReservedQuantity()).isZero();
            assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("일부만 해제하면 나머지 예약이 남는다")
        void partialReleaseShouldLeaveRemainingReservedQuantity() {
            inventory.releaseReservation(30);
            assertThat(inventory.getReservedQuantity()).isEqualTo(30);
        }

        @Test
        @DisplayName("예약 수량보다 많이 해제하면 ExcessiveReservationReleaseException이 발생한다")
        void excessiveReleaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.releaseReservation(100))
                    .isInstanceOf(ExcessiveReservationReleaseException.class);
        }

        @Test
        @DisplayName("예약 수량 초과 해제 실패 시 기존 예약 수량은 그대로 유지된다")
        void failedExcessiveReleaseShouldNotChangeReservedQuantity() {
            try {
                inventory.releaseReservation(100);
            } catch (ExcessiveReservationReleaseException ignored) {
            }
            assertThat(inventory.getReservedQuantity()).isEqualTo(60);
        }

        @Test
        @DisplayName("0을 해제하면 예외가 발생한다")
        void zeroQuantityReleaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.releaseReservation(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수를 해제하면 예외가 발생한다")
        void negativeQuantityReleaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.releaseReservation(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("가용 재고 계산")
    class AvailableQuantity {

        @Test
        @DisplayName("초기 가용 재고는 0이다")
        void initialAvailableQuantityShouldBeZero() {
            assertThat(inventory.getAvailableQuantity()).isZero();
        }

        @Test
        @DisplayName("가용 재고는 일반 재고에서 예약 수량을 뺀 값이다")
        void availableQuantityShouldBeNormalMinusReserved() {
            inventory.increaseNormal(100);
            inventory.reserve(30);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("불량 재고와 검수 대기 재고는 가용 재고에 포함되지 않는다")
        void damagedAndPendingInspectionStockShouldNotAffectAvailableQuantity() {
            inventory.increaseNormal(100);
            inventory.increaseDamaged(50);
            inventory.increasePendingInspection(30);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        }
    }
}
