package com.kb.wmslab.goods_wms.concurrency;

import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRepository;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LostUpdateTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 동시_수정시_한쪽_변경이_예외_없이_유실된다() throws InterruptedException {
        // given
        Member saved = memberRepository.save(Member.create("홍길동", "concurrent@test.com", MemberRole.ADMIN));
        Long id = saved.getId();
        System.out.println("\n[초기 상태] id=" + id + ", role=" + saved.getRole());

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        AtomicInteger writeOrder = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            Member memberA = memberRepository.findById(id).orElseThrow();
            System.out.println("[스레드 A] 읽기 완료 - role=" + memberA.getRole());
            readyLatch.countDown();
            try {
                startLatch.await();
                memberA.changeRole(MemberRole.WAREHOUSE_MANAGER);
                memberRepository.save(memberA);
                int order = writeOrder.incrementAndGet();
                System.out.println("[스레드 A] 저장 완료 (쓰기 순서 " + order + "번째) - role=" + MemberRole.WAREHOUSE_MANAGER);
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
                System.out.println("[스레드 A] 예외 발생 - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        executor.submit(() -> {
            Member memberB = memberRepository.findById(id).orElseThrow();
            System.out.println("[스레드 B] 읽기 완료 - role=" + memberB.getRole());
            readyLatch.countDown();
            try {
                startLatch.await();
                memberB.changeRole(MemberRole.OUTBOUND_HANDLER);
                memberRepository.save(memberB);
                int order = writeOrder.incrementAndGet();
                System.out.println("[스레드 B] 저장 완료 (쓰기 순서 " + order + "번째) - role=" + MemberRole.OUTBOUND_HANDLER);
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
                System.out.println("[스레드 B] 예외 발생 - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        readyLatch.await();
        System.out.println("\n[두 스레드 모두 읽기 완료 → 동시 쓰기 시작]\n");
        startLatch.countDown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Member result = memberRepository.findById(id).orElseThrow();
        System.out.println("\n[최종 상태] role=" + result.getRole());
        System.out.println("[예외 발생 수] " + exceptionCount.get() + " (0이면 Lost Update 발생)");

        // then: 두 요청 모두 예외 없이 성공했지만 한 쪽 변경이 유실됨
        assertThat(exceptionCount.get()).isEqualTo(0);
    }
}

/**
 * [초기 상태] id=1, role=ADMIN
 * [스레드 A] 읽기 완료 - role=ADMIN
 * [스레드 B] 읽기 완료 - role=ADMIN
 *
 * [두 스레드 모두 읽기 완료 → 동시 쓰기 시작]
 *
 * [스레드 B] 저장 완료 (쓰기 순서 1번째) - role=OUTBOUND_HANDLER
 * [스레드 A] 저장 완료 (쓰기 순서 2번째) - role=WAREHOUSE_MANAGER
 *
 * [최종 상태] role=WAREHOUSE_MANAGER
 * [예외 발생 수] 0 (0이면 Lost Update 발생)
 */
