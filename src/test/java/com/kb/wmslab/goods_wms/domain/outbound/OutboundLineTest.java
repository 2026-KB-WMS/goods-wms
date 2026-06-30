package com.kb.wmslab.goods_wms.domain.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OutboundLine 도메인")
class OutboundLineTest {

    @Nested
    @DisplayName("OutboundLine 생성")
    class Create {

        @Test
        @DisplayName("양수 수량으로 라인을 생성할 수 있다")
        void outboundLineShouldBeCreatedWithPositiveQuantity() {
            OutboundLine line = OutboundLine.create(1L, 10);
            assertThat(line.getQuantity()).isEqualTo(10);
            assertThat(line.getProductId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("수량이 0이면 예외가 발생한다")
        void creatingOutboundLineWithZeroQuantityShouldThrowException() {
            assertThatThrownBy(() -> OutboundLine.create(1L, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("수량이 음수이면 예외가 발생한다")
        void creatingOutboundLineWithNegativeQuantityShouldThrowException() {
            assertThatThrownBy(() -> OutboundLine.create(1L, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("OutboundLine zone 할당")
    class AssignZone {

        @Test
        @DisplayName("신규 생성 시 zoneId는 null이다")
        void newlyCreatedLineShouldHaveNullZoneId() {
            OutboundLine line = OutboundLine.create(1L, 10);
            assertThat(line.getZoneId()).isNull();
        }

        @Test
        @DisplayName("assignZone 호출 시 zoneId가 설정된다")
        void assignZoneShouldSetZoneId() {
            OutboundLine line = OutboundLine.create(1L, 10);
            line.assignZone(100L);
            assertThat(line.getZoneId()).isEqualTo(100L);
        }
    }
}
