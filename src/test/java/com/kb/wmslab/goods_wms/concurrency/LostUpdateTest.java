package com.kb.wmslab.goods_wms.concurrency;

import com.kb.wmslab.goods_wms.business.domain.member.Member;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRepository;
import com.kb.wmslab.goods_wms.business.domain.member.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LostUpdateTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 동시_수정시_충돌이_감지되어_OptimisticLockingFailureException이_발생한다() throws InterruptedException {
        // given
        Member saved = memberRepository.save(Member.create("홍길동", "concurrent@test.com", MemberRole.ADMIN));
        Long id = saved.getId();
        System.out.println("\n[초기 상태] id=" + id + ", role=" + saved.getRole());

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        AtomicInteger writeOrder = new AtomicInteger(0);
        AtomicReference<Class<? extends Exception>> caughtExceptionType = new AtomicReference<>();

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
                caughtExceptionType.set(e.getClass());
                System.out.println("[스레드 A] 예외 발생 - " + e.getClass().getSimpleName());
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
                caughtExceptionType.set(e.getClass());
                System.out.println("[스레드 B] 예외 발생 - " + e.getClass().getSimpleName());
            }
        });

        readyLatch.await();
        System.out.println("\n[두 스레드 모두 읽기 완료 → 동시 쓰기 시작]\n");
        startLatch.countDown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Member result = memberRepository.findById(id).orElseThrow();
        System.out.println("\n[최종 상태] role=" + result.getRole());
        System.out.println("[예외 발생 수] " + exceptionCount.get());

        // then: 한 요청은 성공, 다른 요청은 충돌을 감지하여 예외 발생
        assertThat(exceptionCount.get()).isEqualTo(1);
        assertThat(caughtExceptionType.get()).isEqualTo(ObjectOptimisticLockingFailureException.class);
    }
}

/**
 * [초기 상태] id=1, role=ADMIN
 *
 * [스레드 A] 읽기 완료 - role=ADMIN
 * [스레드 B] 읽기 완료 - role=ADMIN
 *
 * [두 스레드 모두 읽기 완료 → 동시 쓰기 시작]
 *
 * [스레드 B] 저장 완료 (쓰기 순서 1번째) - role=OUTBOUND_HANDLER
 * [스레드 A] 예외 발생 - ObjectOptimisticLockingFailureException
 *
 * [최종 상태] role=OUTBOUND_HANDLER
 * [예외 발생 수] 1
 */
