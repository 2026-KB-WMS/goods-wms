package com.kb.wmslab.goods_wms.domain.outbound;

import com.kb.wmslab.goods_wms.business.domain.common.exception.EmptyOutboundLineException;
import com.kb.wmslab.goods_wms.business.domain.common.exception.InvalidStatusTransitionException;
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

@DisplayName("Outbound 상태 전이 (검증 / 완료 / 취소)")
class OutboundStatusTransitionTest {

    private Outbound outbound;

    @BeforeEach
    void setUp() {
        outbound = Outbound.create(1L, 1L, "서울 행사장", OutboundPurpose.EVENT_BOOTH);
    }

    @Nested
    @DisplayName("출고 검증")
    class Validate {

        @Test
        @DisplayName("CREATED 상태에서 라인이 있으면 검증에 성공한다")
        void outboundShouldValidateWhenStatusIsCreatedAndLinesExist() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.VALIDATED);
        }

        @Test
        @DisplayName("CREATED 상태에서 라인이 없으면 예외가 발생한다")
        void outboundShouldNotValidateWhenStatusIsCreatedAndLinesAreEmpty() {
            assertThatThrownBy(() -> outbound.validate())
                    .isInstanceOf(EmptyOutboundLineException.class);
        }

        @Test
        @DisplayName("VALIDATED 상태에서 다시 검증하면 예외가 발생한다")
        void validatingWhenOutboundStatusIsValidatedShouldThrowException() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            assertThatThrownBy(() -> outbound.validate())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 검증하면 예외가 발생한다")
        void validatingWhenOutboundStatusIsCompletedShouldThrowException() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            outbound.complete();
            assertThatThrownBy(() -> outbound.validate())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 검증하면 예외가 발생한다")
        void validatingWhenOutboundStatusIsCanceledShouldThrowException() {
            outbound.cancel();
            assertThatThrownBy(() -> outbound.validate())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("출고 완료")
    class Complete {

        @Test
        @DisplayName("VALIDATED 상태에서 출고를 완료할 수 있다")
        void outboundShouldCompleteWhenStatusIsValidated() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            outbound.complete();
            assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.COMPLETED);
        }

        @Test
        @DisplayName("CREATED 상태에서 완료하면 예외가 발생한다")
        void completingOutboundWhenStatusIsCreatedShouldThrowException() {
            assertThatThrownBy(() -> outbound.complete())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 다시 완료하면 예외가 발생한다")
        void completingOutboundWhenStatusIsCompletedShouldThrowException() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            outbound.complete();
            assertThatThrownBy(() -> outbound.complete())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 완료하면 예외가 발생한다")
        void completingOutboundWhenStatusIsCanceledShouldThrowException() {
            outbound.cancel();
            assertThatThrownBy(() -> outbound.complete())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("출고 취소")
    class Cancel {

        @Test
        @DisplayName("CREATED 상태에서 취소할 수 있다")
        void outboundShouldCancelWhenStatusIsCreated() {
            outbound.cancel();
            assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.CANCELED);
        }

        @Test
        @DisplayName("VALIDATED 상태에서 취소할 수 있다")
        void outboundShouldCancelWhenStatusIsValidated() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            outbound.cancel();
            assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.CANCELED);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 취소하면 예외가 발생한다")
        void cancellingOutboundWhenStatusIsCompletedShouldThrowException() {
            outbound.addLine(OutboundLine.create(1L, 10));
            outbound.validate();
            outbound.complete();
            assertThatThrownBy(() -> outbound.cancel())
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 다시 취소해도 상태는 CANCELED로 유지된다")
        void cancellingAlreadyCancelledOutboundShouldKeepStatusCanceled() {
            outbound.cancel();
            outbound.cancel();
            assertThat(outbound.getStatus()).isEqualTo(OutboundStatus.CANCELED);
        }
    }
}
