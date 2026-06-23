package com.kb.wmslab.goods_wms.domain.outbound;

import com.kb.wmslab.goods_wms.business.domain.outbound.Outbound;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundLine;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundPurpose;
import com.kb.wmslab.goods_wms.business.domain.outbound.OutboundStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Outbound 구성 (생성 / 라인 추가)")
class OutboundCompositionTest {

    private Outbound outbound;

    @BeforeEach
    void setUp() {
        outbound = Outbound.create(1L, 1L, "서울 행사장", OutboundPurpose.EVENT_BOOTH);
    }

    @Nested
    @DisplayName("출고 생성")
    class Create {

        @Test
        @DisplayName("생성 시 상태는 CREATED이다")
        void newlyCreatedOutboundShouldHaveCreatedStatus() {
            assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.CREATED);
        }

        @Test
        @DisplayName("생성 시 라인 목록은 비어있다")
        void newlyCreatedOutboundShouldHaveEmptyLines() {
            assertThat(outbound.getLines()).isEmpty();
        }

        @Test
        @DisplayName("생성 시 id는 null이다")
        void newlyCreatedOutboundShouldHaveNullId() {
            assertThat(outbound.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("라인 추가")
    class AddLine {

        @Test
        @DisplayName("CREATED 상태에서 라인을 추가할 수 있다")
        void outboundShouldAllowAddingLineWhenStatusIsCreated() {
            outbound.addLine(OutboundLine.create(1L, 10));
            assertThat(outbound.getLines()).hasSize(1);
        }

        @Test
        @DisplayName("라인을 여러 개 추가할 수 있다")
        void outboundShouldAllowAddingMultipleLines() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.addLine(OutboundLine.create(2L, 5));
            assertThat(outbound.getLines()).hasSize(2);
        }

        @Test
        @DisplayName("VALIDATED 상태에서 라인을 추가하면 예외가 발생한다")
        void addingLineWhenOutboundStatusIsValidatedShouldThrowException() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            assertThatThrownBy(() -> outbound.addLine(OutboundLine.create(2L, 5)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 라인을 추가하면 예외가 발생한다")
        void addingLineWhenOutboundStatusIsCompletedShouldThrowException() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            outbound.complete();
            assertThatThrownBy(() -> outbound.addLine(OutboundLine.create(2L, 5)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 라인을 추가하면 예외가 발생한다")
        void addingLineWhenOutboundStatusIsCanceledShouldThrowException() {
            outbound.cancel();
            assertThatThrownBy(() -> outbound.addLine(OutboundLine.create(1L, 10)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("getLines()는 수정 불가능한 리스트를 반환한다")
        void getLinesShouldReturnUnmodifiableList() {
            outbound.addLine(OutboundLine.create(1L, 10));
            assertThatThrownBy(() -> outbound.getLines().add(OutboundLine.create(2L, 5)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
