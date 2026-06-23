package com.kb.wmslab.goods_wms.domain.inbound;

import com.kb.wmslab.goods_wms.business.domain.inbound.Inbound;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundLine;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inbound 구성 (생성 / 라인 추가)")
class InboundCompositionTest {

    private Inbound inbound;

    @BeforeEach
    void setUp() {
        inbound = Inbound.create(1L, 1L, "공급사A");
    }

    @Nested
    @DisplayName("입고 생성")
    class Create {

        @Test
        @DisplayName("생성 시 상태는 CREATED이다")
        void newlyCreatedInboundShouldHaveCreatedStatus() {
            assertThat(inbound.getStatus()).isEqualTo(InboundStatus.CREATED);
        }

        @Test
        @DisplayName("생성 시 라인 목록은 비어있다")
        void newlyCreatedInboundShouldHaveEmptyLines() {
            assertThat(inbound.getLines()).isEmpty();
        }

        @Test
        @DisplayName("생성 시 id는 null이다")
        void newlyCreatedInboundShouldHaveNullId() {
            assertThat(inbound.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("라인 추가")
    class AddLine {

        @Test
        @DisplayName("CREATED 상태에서 라인을 추가할 수 있다")
        void inboundShouldAllowAddingLineWhenStatusIsCreated() {
            inbound.addLine(InboundLine.create(1L, 10));
            assertThat(inbound.getLines()).hasSize(1);
        }

        @Test
        @DisplayName("라인을 여러 개 추가할 수 있다")
        void inboundShouldAllowAddingMultipleLines() {
            inbound.addLine(InboundLine.create(1L, 10));
            inbound.addLine(InboundLine.create(2L, 20));
            assertThat(inbound.getLines()).hasSize(2);
        }

        @Test
        @DisplayName("INSPECTING 상태에서 라인을 추가하면 예외가 발생한다")
        void addingLineWhenInboundStatusIsInspectingShouldThrowException() {
            inbound.addLine(InboundLine.create(1L, 10));
            inbound.startInspection();
            assertThatThrownBy(() -> inbound.addLine(InboundLine.create(2L, 5)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 라인을 추가하면 예외가 발생한다")
        void addingLineWhenInboundStatusIsCompletedShouldThrowException() {
            InboundLine line = InboundLine.create(1L, 10);
            inbound.addLine(line);
            inbound.startInspection();
            line.recordInspectionResult(10, 0, 0);
            inbound.complete();
            assertThatThrownBy(() -> inbound.addLine(InboundLine.create(2L, 5)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 라인을 추가하면 예외가 발생한다")
        void addingLineWhenInboundStatusIsCanceledShouldThrowException() {
            inbound.cancel();
            assertThatThrownBy(() -> inbound.addLine(InboundLine.create(1L, 10)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("getLines()는 수정 불가능한 리스트를 반환한다")
        void getLinesShouldReturnUnmodifiableList() {
            inbound.addLine(InboundLine.create(1L, 10));
            assertThatThrownBy(() -> inbound.getLines().add(InboundLine.create(2L, 5)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
