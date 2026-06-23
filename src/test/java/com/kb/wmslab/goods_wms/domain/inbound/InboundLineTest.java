package com.kb.wmslab.goods_wms.domain.inbound;

import com.kb.wmslab.goods_wms.business.domain.common.exception.InspectionQuantityMismatchException;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InboundLine 도메인")
class InboundLineTest {

    @Nested
    @DisplayName("InboundLine 검수 기록")
    class RecordInspectionResult {

        @Test
        @DisplayName("검수 수량 합계가 입고 수량과 일치하면 성공한다")
        void recordingInspectionShouldSucceedWhenSumMatchesOrderedQuantity() {
            InboundLine line = InboundLine.create(1L, 10);
            line.recordInspectionResult(7, 2, 1);
            assertThat(line.isInspected()).isTrue();
            assertThat(line.getNormalQuantity()).isEqualTo(7);
            assertThat(line.getDamagedQuantity()).isEqualTo(2);
            assertThat(line.getPendingInspectionQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("전량 정상 처리할 수 있다")
        void recordingInspectionShouldAllowAllNormal() {
            InboundLine line = InboundLine.create(1L, 10);
            line.recordInspectionResult(10, 0, 0);
            assertThat(line.isInspected()).isTrue();
            assertThat(line.getNormalQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("전량 불량 처리할 수 있다")
        void recordingInspectionShouldAllowAllDamaged() {
            InboundLine line = InboundLine.create(1L, 10);
            line.recordInspectionResult(0, 10, 0);
            assertThat(line.isInspected()).isTrue();
            assertThat(line.getDamagedQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("검수 수량 합계가 입고 수량과 다르면 InspectionQuantityMismatchException이 발생한다")
        void recordingInspectionShouldThrowExceptionWhenSumIsLessThanOrderedQuantity() {
            InboundLine line = InboundLine.create(1L, 10);
            assertThatThrownBy(() -> line.recordInspectionResult(5, 2, 1))
                    .isInstanceOf(InspectionQuantityMismatchException.class);
        }

        @Test
        @DisplayName("검수 수량 합계가 입고 수량을 초과하면 InspectionQuantityMismatchException이 발생한다")
        void recordingInspectionShouldThrowExceptionWhenSumExceedsOrderedQuantity() {
            InboundLine line = InboundLine.create(1L, 10);
            assertThatThrownBy(() -> line.recordInspectionResult(10, 5, 0))
                    .isInstanceOf(InspectionQuantityMismatchException.class);
        }

        @Test
        @DisplayName("검수 전에는 inspected가 false이다")
        void inboundLineShouldBeNotInspectedBeforeRecording() {
            InboundLine line = InboundLine.create(1L, 10);
            assertThat(line.isInspected()).isFalse();
        }
    }
}
