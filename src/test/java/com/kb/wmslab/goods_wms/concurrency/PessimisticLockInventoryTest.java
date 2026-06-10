package com.kb.wmslab.goods_wms.concurrency;

import com.kb.wmslab.goods_wms.business.domain.inventory.Inventory;
import com.kb.wmslab.goods_wms.business.domain.inventory.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PessimisticLockInventoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 비관적락_적용시_동시_재고_차감_요청이_모두_정상_처리된다() throws InterruptedException {
        // given
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        long warehouseId = 1L;
        long zoneId = 1L;
        long productId = System.currentTimeMillis();

        txTemplate.execute(status -> {
            Inventory inv = Inventory.create(warehouseId, zoneId, productId);
            Inventory saved = inventoryRepository.save(inv);
            saved.increaseNormal(100);
            inventoryRepository.save(saved);
            return null;
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        System.out.println("[초기 재고] 100개");

        int threadCount = 10;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger processOrder = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    txTemplate.execute(status -> {
                        Inventory inv = inventoryRepository
                                .findByWarehouseIdAndZoneIdAndProductIdForUpdate(warehouseId, zoneId, productId)
                                .orElseThrow();
                        inv.decreaseNormal(10);
                        inventoryRepository.save(inv);
                        int seq = processOrder.incrementAndGet();
                        System.out.println("[처리 " + seq + "번째] " + LocalTime.now().format(formatter)
                                + " 차감 완료 → 남은 재고: " + inv.getNormalQuantity() + "개");
                        return null;
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("[실패] " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            });
        }

        readyLatch.await();
        System.out.println("\n[10개 스레드 동시 차감 시작]\n");
        startLatch.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        AtomicInteger finalQuantity = new AtomicInteger();
        txTemplate.execute(status -> {
            Inventory result = inventoryRepository
                    .findByWarehouseIdAndZoneIdAndProductId(warehouseId, zoneId, productId)
                    .orElseThrow();
            finalQuantity.set(result.getNormalQuantity());
            return null;
        });

        System.out.println("\n[최종 재고] " + finalQuantity.get() + "개");
        System.out.println("[성공 횟수] " + successCount.get());
        System.out.println("[실패 횟수] " + failCount.get());

        // then: 10개 스레드 모두 성공, 재고 정확히 0개
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(finalQuantity.get()).isEqualTo(0);
    }
}

/**
 * [초기 재고] 100개
 *
 * [10개 스레드 동시 차감 시작]
 *
 * [처리 1번째] 14:16:52.009 차감 완료 → 남은 재고: 90개
 * [처리 2번째] 14:16:52.013 차감 완료 → 남은 재고: 80개
 * [처리 3번째] 14:16:52.015 차감 완료 → 남은 재고: 70개
 * [처리 4번째] 14:16:52.017 차감 완료 → 남은 재고: 60개
 * [처리 5번째] 14:16:52.020 차감 완료 → 남은 재고: 50개
 * [처리 6번째] 14:16:52.022 차감 완료 → 남은 재고: 40개
 * [처리 7번째] 14:16:52.024 차감 완료 → 남은 재고: 30개
 * [처리 8번째] 14:16:52.026 차감 완료 → 남은 재고: 20개
 * [처리 9번째] 14:16:52.029 차감 완료 → 남은 재고: 10개
 * [처리 10번째] 14:16:52.031 차감 완료 → 남은 재고: 0개
 *
 * [최종 재고] 0개
 * [성공 횟수] 10
 * [실패 횟수] 0
 */
