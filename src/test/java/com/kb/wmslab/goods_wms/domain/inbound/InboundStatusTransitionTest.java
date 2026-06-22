package com.kb.wmslab.goods_wms.domain.inbound;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EmptyInboundLineException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.IncompleteInspectionException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InvalidStatusTransitionException;
import com.kb.wmslab.goods_wms.business.domain.inbound.Inbound;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundLine;
import com.kb.wmslab.goods_wms.business.domain.inbound.InboundStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inbound 상태 전이 (검수 시작 / 완료 / 취소)")
class InboundStatusTransitionTest {

    private Inbound inbound;

    @BeforeEach
    void setUp() {
        inbound = Inbound.create(1L, 1L, "공급사A");
    }

    @Nested
    @DisplayName("검수 시작")
    class StartInspection {

        @Test
        @DisplayName("CREATED 상태에서 라인이 있으면 검수를 시작할 수 있다")
        void inboundShouldStartInspectionWhenStatusIsCreatedAndLinesExist() {
            inbound.addLine(InboundLine.create(1L, 10));
            inbound.startInspection();
            assertThat(inbound.getStatus()).isEqualTo(InboundStatus.INSPECTING);
        }

        @Test
        @DisplayName("CREATED 상태에서 라인이 없으면 EmptyInboundLineException이 발생한다")
        void inboundShouldNotStartInspectionWhenStatusIsCreatedAndLinesAreEmpty() {
            assertThatThrownBy(() -> inbound.startInspection())
                    .isInstanceOf(EmptyInboundLineException.class);
        }

        @Test
        @DisplayName("INSPECTING 상태에서 검수를 시작하면 예외가 발생한다")
        void startingInspectionWhenInboundStatusIsInspectingShouldThrowException() {
            inbound.addLine(InboundLine.create(1L, 10));
            inbound.startInspection();
            assertThatThrownBy(() -> inbound.startInspection())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 검수를 시작하면 예외가 발생한다")
        void startingInspectionWhenInboundStatusIsCompletedShouldThrowException() {
            InboundLine line = InboundLine.create(1L, 10);
            inbound.addLine(line);
            inbound.startInspection();
            line.recordInspectionResult(10, 0, 0);
            inbound.complete();
            assertThatThrownBy(() -> inbound.startInspection())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 검수를 시작하면 예외가 발생한다")
        void startingInspectionWhenInboundStatusIsCanceledShouldThrowException() {
            inbound.cancel();
            assertThatThrownBy(() -> inbound.startInspection())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("입고 완료")
    class Complete {

        @Test
        @DisplayName("INSPECTING 상태에서 모든 라인이 검수 완료되면 입고를 완료할 수 있다")
        void inboundShouldCompleteWhenStatusIsInspectingAndAllLinesAreInspected() {
            InboundLine line = InboundLine.create(1L, 10);
            inbound.addLine(line);
            inbound.startInspection();
            line.recordInspectionResult(10, 0, 0);
            inbound.complete();
            assertThat(inbound.getStatus()).isEqualTo(InboundStatus.COMPLETED);
        }

        @Test
        @DisplayName("INSPECTING 상태에서 검수되지 않은 라인이 있으면 IncompleteInspectionException이 발생한다")
        void completingInboundShouldThrowExceptionWhenAnyLineIsNotInspected() {
            inbound.addLine(InboundLine.create(1L, 10));
            inbound.addLine(InboundLine.create(2L, 5));
            inbound.startInspection();
            inbound.getLines().get(0).recordInspectionResult(10, 0, 0);
            assertThatThrownBy(() -> inbound.complete())
                    .isInstanceOf(IncompleteInspectionException.class);
        }

        @Test
        @DisplayName("CREATED 상태에서 완료하면 예외가 발생한다")
        void completingInboundWhenStatusIsCreatedShouldThrowException() {
            assertThatThrownBy(() -> inbound.complete())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 완료하면 예외가 발생한다")
        void completingInboundWhenStatusIsCompletedShouldThrowException() {
            InboundLine line = InboundLine.create(1L, 10);
            inbound.addLine(line);
            inbound.startInspection();
            line.recordInspectionResult(10, 0, 0);
            inbound.complete();
            assertThatThrownBy(() -> inbound.complete())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 완료하면 예외가 발생한다")
        void completingInboundWhenStatusIsCanceledShouldThrowException() {
            inbound.cancel();
            assertThatThrownBy(() -> inbound.complete())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("입고 취소")
    class Cancel {

        @Test
        @DisplayName("CREATED 상태에서 취소할 수 있다")
        void inboundShouldCancelWhenStatusIsCreated() {
            inbound.cancel();
            assertThat(inbound.getStatus()).isEqualTo(InboundStatus.CANCELED);
        }

        @Test
        @DisplayName("INSPECTING 상태에서 취소할 수 있다")
        void inboundShouldCancelWhenStatusIsInspecting() {
            inbound.addLine(InboundLine.create(1L, 10));
            inbound.startInspection();
            inbound.cancel();
            assertThat(inbound.getStatus()).isEqualTo(InboundStatus.CANCELED);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 취소하면 예외가 발생한다")
        void cancellingInboundWhenStatusIsCompletedShouldThrowException() {
            InboundLine line = InboundLine.create(1L, 10);
            inbound.addLine(line);
            inbound.startInspection();
            line.recordInspectionResult(10, 0, 0);
            inbound.complete();
            assertThatThrownBy(() -> inbound.cancel())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 다시 취소해도 상태는 CANCELED로 유지된다")
        void cancellingAlreadyCancelledInboundShouldKeepStatusCanceled() {
            inbound.cancel();
            inbound.cancel();
            assertThat(inbound.getStatus()).isEqualTo(InboundStatus.CANCELED);
        }
    }
}
