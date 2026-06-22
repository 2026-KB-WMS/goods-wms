package com.kb.wmslab.goods_wms.domain.inventory;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InsufficientStockException;
import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inventory 재고 변동 (생성 / 증가 / 차감)")
class InventoryStockChangeTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = Inventory.create(1L, 1L, 1L);
    }

    @Nested
    @DisplayName("재고 생성")
    class Create {

        @Test
        @DisplayName("초기 재고는 모든 수량이 0이다")
        void initialInventoryShouldHaveAllQuantitiesZero() {
            assertThat(inventory.getNormalQuantity()).isZero();
            assertThat(inventory.getDamagedQuantity()).isZero();
            assertThat(inventory.getPendingInspectionQuantity()).isZero();
            assertThat(inventory.getReservedQuantity()).isZero();
        }

        @Test
        @DisplayName("신규 생성 시 id는 null이다")
        void newlyCreatedInventoryShouldHaveNullId() {
            assertThat(inventory.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("일반 재고 증가")
    class IncreaseNormal {

        @Test
        @DisplayName("양수 수량만큼 일반 재고가 증가한다")
        void normalStockShouldIncreaseByPositiveQuantity() {
            inventory.increaseNormal(50);
            assertThat(inventory.getNormalQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("여러 번 증가하면 누적된다")
        void normalStockShouldAccumulateWhenIncreasedMultipleTimes() {
            inventory.increaseNormal(30);
            inventory.increaseNormal(20);
            assertThat(inventory.getNormalQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("0을 증가시키면 예외가 발생한다")
        void zeroQuantityIncreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.increaseNormal(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수를 증가시키면 예외가 발생한다")
        void negativeQuantityIncreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.increaseNormal(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("불량 재고 증가")
    class IncreaseDamaged {

        @Test
        @DisplayName("양수 수량만큼 불량 재고가 증가한다")
        void damagedStockShouldIncreaseByPositiveQuantity() {
            inventory.increaseDamaged(10);
            assertThat(inventory.getDamagedQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("0을 증가시키면 예외가 발생한다")
        void zeroQuantityIncreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.increaseDamaged(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수를 증가시키면 예외가 발생한다")
        void negativeQuantityIncreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.increaseDamaged(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("검수 대기 재고 증가")
    class IncreasePendingInspection {

        @Test
        @DisplayName("양수 수량만큼 검수 대기 재고가 증가한다")
        void pendingInspectionStockShouldIncreaseByPositiveQuantity() {
            inventory.increasePendingInspection(5);
            assertThat(inventory.getPendingInspectionQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("0을 증가시키면 예외가 발생한다")
        void zeroQuantityIncreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.increasePendingInspection(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수를 증가시키면 예외가 발생한다")
        void negativeQuantityIncreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.increasePendingInspection(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("일반 재고 차감")
    class DecreaseNormal {

        @BeforeEach
        void setUp() {
            inventory.increaseNormal(100);
        }

        @Test
        @DisplayName("가용 재고 이하로 차감하면 성공한다")
        void stockDecreaseShouldSucceedWhenQuantityIsWithinAvailableStock() {
            inventory.decreaseNormal(100);
            assertThat(inventory.getNormalQuantity()).isZero();
        }

        @Test
        @DisplayName("일부만 차감하면 나머지가 남는다")
        void decreasingPartialQuantityShouldLeaveRemainingStock() {
            inventory.decreaseNormal(40);
            assertThat(inventory.getNormalQuantity()).isEqualTo(60);
        }

        @Test
        @DisplayName("가용 재고를 초과하면 InsufficientStockException이 발생한다")
        void decreasingMoreThanAvailableQuantityShouldThrowException() {
            assertThatThrownBy(() -> inventory.decreaseNormal(101))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("예약 수량을 제외한 가용 재고 초과 시 InsufficientStockException이 발생한다")
        void decreasingBeyondAvailableStockExcludingReservedQuantityShouldThrowException() {
            inventory.reserve(60);
            assertThatThrownBy(() -> inventory.decreaseNormal(50))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("0을 차감하면 예외가 발생한다")
        void zeroQuantityDecreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.decreaseNormal(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수를 차감하면 예외가 발생한다")
        void negativeQuantityDecreaseShouldThrowException() {
            assertThatThrownBy(() -> inventory.decreaseNormal(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
