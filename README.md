# Goods WMS

캐릭터 굿즈 및 한정판 굿즈를 위한 재고 관리 시스템(Warehouse Management System)

---

## 프로젝트 목적

단순 수량 기록이 아닌, **누가 / 어느 창고에 / 무슨 상품을 / 얼마나 / 왜 입출고했는지**를 추적하고 재고 정합성을 안정적으로 유지하는 굿즈 도메인 특화 재고 관리 시스템.

한정판/콜라보 굿즈 출시 시점에 발생하는 대량 트래픽과 동시성 이슈를 안정적으로 처리하는 것이 핵심 과제다.

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5, Spring Data JPA, Spring Batch, Spring Cache |
| Database | MySQL 8 |
| Cache | Redis 7 |
| API 문서화 | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, AssertJ |
| Build | Gradle |
| 기타 | Lombok |

---

## 아키텍처

**3-tier + DDD(Domain-Driven Design)**

```
controller/                 HTTP 요청/응답, Controller, Request DTO
business/
  application/              유스케이스 조합, 트랜잭션 관리, Command/Result
  domain/                   핵심 비즈니스 규칙, 애그리거트, Repository 인터페이스, Domain Exception
repository/                 JPA Entity, JpaRepository, Domain ↔ Persistence 매핑
batch/                      Spring Batch Job/Step, Reader/Processor/Writer
config/                     Redis, Cache, RecordTypeResolver 등 인프라 설정
```

도메인 객체는 JPA 어노테이션과 분리하여 순수 Java 객체로 작성하고, `JpaEntity` ↔ `Domain` 매핑 레이어로 분리하여 도메인의 순수성과 테스트 용이성을 확보했다.

---

## 바운디드 컨텍스트 & 도메인 모델

### Member — 내부 운영 인력 관리
| 클래스 | 역할 |
|---|---|
| `Member` | 애그리거트 루트. 역할/상태 관리, 업무 수행 가능 여부 검증 |
| `MemberRole` | `ADMIN`, `WAREHOUSE_MANAGER`, `INBOUND_HANDLER`, `OUTBOUND_HANDLER` |
| `MemberStatus` | `ACTIVE`, `INACTIVE` |

### Product — 굿즈 상품 식별 및 카탈로그
| 클래스 | 역할 |
|---|---|
| `Product` | 애그리거트 루트. SKU, 에디션, 한정판 여부, 상품 유형 관리 |
| `ProductType` | `ACRYLIC_STAND`, `KEYRING`, `FIGURE`, `PHOTO_CARD`, `PLUSH`, `RANDOM_BOX`, `LIMITED_PACKAGE`, `OTHER` |
| `ProductStatus` | `ACTIVE`, `INACTIVE` |

### Warehouse — 창고 및 보관 구역
| 클래스 | 역할 |
|---|---|
| `Warehouse` | 애그리거트 루트. 활성/비활성, 구역 추가, 구역 코드 중복 방지 |
| `WarehouseZone` | 창고 내 보관 구역 (Child Entity) |
| `ZoneType` | `NORMAL`, `DAMAGED`, `SECURE` |

### Inbound — 입고 문서 및 검수 흐름
| 클래스 | 역할 |
|---|---|
| `Inbound` | 애그리거트 루트. 입고 문서 생성, 상태 전이 관리 |
| `InboundLine` | 상품별 입고 라인, 검수 결과(정상/파손/검수대기) 기록 |
| `InboundStatus` | `CREATED` → `INSPECTING` → `COMPLETED` / `CANCELED` |

### Outbound — 출고 요청 및 완료 흐름
| 클래스 | 역할 |
|---|---|
| `Outbound` | 애그리거트 루트. 출고 문서 생성, 상태 전이 관리 |
| `OutboundLine` | 상품별 출고 수량 |
| `OutboundStatus` | `CREATED` → `VALIDATED` → `COMPLETED` / `CANCELED` |
| `OutboundPurpose` | `POPUP_STORE`, `EVENT_BOOTH`, `ONLINE_SALES`, `EXCHANGE`, `RETURN`, `DISPOSAL` |

### Inventory — 창고/구역/상품 기준 현재 재고
| 클래스 | 역할 |
|---|---|
| `Inventory` | 애그리거트 루트. 상태별 재고 수량, 가용 재고 계산, 음수 재고 방지 |
| `StockStatus` | `NORMAL`, `DAMAGED`, `PENDING_INSPECTION`, `RESERVED` |

---

## 핵심 비즈니스 규칙

- 재고는 **입고 완료** 또는 **출고 완료**를 통해서만 변경된다
- 출고 가능 여부는 총재고가 아닌 **가용 재고(정상 - 예약)** 기준으로 판단한다
- 파손/검수대기 재고는 일반 출고 대상이 아니다
- 재고는 음수가 될 수 없다
- 비활성 창고/운영 인력은 신규 입출고 대상이 될 수 없다
- 동일 창고 내 구역 코드는 중복될 수 없다
- 이미 완료된 입고/출고 문서는 재완료/일반 취소 불가
- 비즈니스 규칙 위반은 도메인 전용 커스텀 예외(`DomainException` 계층)로 표현

---

## 주요 기술 도입

### 1. 동시성 제어

| 영역 | 전략 | 적용 위치 |
|---|---|---|
| 일반 재고 수정 | `@Version` 낙관적 락 | `InventoryJpaEntity`, `Inbound`, `Outbound` |
| 출고 승인 (재고 부족 직접 발생 구간) | `SELECT ... FOR UPDATE` 비관적 락 | `InventoryRepository.findByWarehouseIdAndZoneIdAndProductIdForUpdate` |

낙관적 락은 충돌 시 재시도/예외 처리가 가능하도록 설계하고, 핵심 구간에는 비관적 락을 적용하여 동시에 같은 재고가 차감되는 문제를 방지한다.

### 2. Redis 캐싱 (Cache-aside)

| 캐시 | 키 | TTL |
|---|---|---|
| `inventory` | `warehouseId:zoneId:productId` | 10분 |
| `inventoryByWarehouse` | `warehouseId` | 10분 |
| `warehouse` | `warehouseId` | 1시간 |
| `warehouseAll` | `'all'` | 1시간 |

읽기 빈도가 높고 쓰기 빈도가 낮은 조회 API에 `@Cacheable`을 적용했다.

**선택적 캐시 무효화** — 기존에는 `@CacheEvict(allEntries = true)`로 전체를 비웠으나, `@CacheEvict`의 `key` SpEL이 메서드 파라미터만 참조할 수 있는 한계로 `InventoryCacheEvictor` 컴포넌트를 도입해 영향받은 키만 선택적으로 무효화한다. 무효화 범위를 O(N) → O(1)로 축소했다.

**트랜잭션 안전성** — `TransactionSynchronizationManager.registerSynchronization`의 `afterCommit()` 콜백을 사용하여 트랜잭션 커밋 이후에만 evict가 실행되도록 보장한다.

**Java record 직렬화** — `GenericJackson2JsonRedisSerializer` + `WRAPPER_ARRAY` 전략 + `RecordSupportingTypeResolver`로 Jackson의 `NON_FINAL` 전략에서 record가 누락되는 문제를 보완하고, `PolymorphicTypeValidator`로 역직렬화 허용 타입을 프로젝트 도메인과 `java.util`로 제한해 보안 취약점을 차단했다.

### 3. 대량 입고 배치 (Spring Batch)

한정판/콜라보 출시 시점의 대량 입고(수만~수십만 건)를 처리하기 위한 CSV 기반 배치.

```
BulkInboundJob
└── bulkInboundStep (@JobScope, chunk-oriented)
    ├── Reader:    SynchronizedItemStreamReader(FlatFileItemReader)
    ├── Processor: 유효성 검증
    ├── Writer:    writerType 파라미터로 JPA / JDBC Bulk 분기
    └── TaskExecutor: threadCount 파라미터로 단일 / 멀티 분기
```

`chunkSize`, `threadCount`, `writerType`을 모두 Job 파라미터로 받아 청크 크기, 멀티스레드, JDBC Bulk insert 적용 여부를 단일 코드에서 비교 측정할 수 있다.

| Endpoint | 설명 |
|---|---|
| `POST /inbounds/bulk` | CSV 업로드 + Job 실행 |
| `GET /inbounds/bulk/jobs/{executionId}` | 실행 상태, durationMs, read/write/skip 카운트 조회 |

### 4. 도메인 단위 테스트

`Inbound`, `Outbound`, `Inventory`의 핵심 비즈니스 규칙을 외부 의존성 없이 검증한다. 의미 단위로 파일을 분리해 가독성과 응집도를 균형 있게 유지했다.

```
src/test/java/.../domain/
├── inbound/
│   ├── InboundCompositionTest         (생성, 라인 추가)
│   ├── InboundStatusTransitionTest    (검수 시작, 완료, 취소)
│   └── InboundLineTest                (라인 검수 기록)
├── inventory/
│   ├── InventoryStockChangeTest       (생성, 증가, 차감)
│   └── InventoryReservationTest       (예약, 해제, 가용 재고)
└── outbound/
    ├── OutboundCompositionTest
    ├── OutboundStatusTransitionTest
    └── OutboundLineTest
```

정상 경로 + 경계값 + 모든 예외 분기를 포함한 80여 개 케이스로 구성된다.

### 5. REST API 및 문서화

- `@RestController` 기반 JSON API 설계
- `@RestControllerAdvice` 기반 전역 예외 처리로 도메인 예외 → HTTP 응답 매핑
- `springdoc-openapi`로 Swagger UI 자동 문서화 — `http://localhost:8080/swagger-ui/index.html`

---

## 성능 개선 사례 (실측)

### 1. Redis 캐싱 도입 (Apache Bench, n=200, c=10)

| Endpoint | TPS (전 → 후) | P99 (전 → 후) |
|---|---|---|
| `GET /inventories/warehouses/{id}` | 1,874 → 2,773 (**+48%**) | 34ms → 7ms (**−79%**) |
| `GET /inventories?...` | 1,960 → 2,855 (**+46%**) | 13ms → 7ms (**−46%**) |
| `GET /warehouses` | 1,792 → 3,361 (**+88%**) | 17ms → 7ms (**−59%**) |
| `GET /warehouses/{id}` | 2,690 → 3,430 (**+28%**) | — |

### 2. 선택적 캐시 무효화

| 측정 | 결과                            |
|---|-------------------------------|
| 무효화 범위 | O(N) → **O(1)**               |
| 창고 2개 환경 | 4개 키 무효화 → 2개로 감소 (**50% ↓**) |
| 영향 없는 창고 캐시 히트율 | 0% (전체 무효화) → **100% 유지**     |

### 3. 대량 입고 배치 (10만 건)

| # | 케이스                              | 처리 시간 | 베이스라인 대비 시간 증감        |
|---|----------------------------------|---|-----------------------|
| #1 | JPA / chunk=1000 / thread=1 (기준) | 26.9초 | -                     |
| #2 | JPA / chunk=100 / thread=1       | 105.7초 | **292% 처리 시간 증가**     |
| #3 | JPA / chunk=10000 / thread=1     | 24.7초 | 8% 처리 시간 감소           |
| #4 | JPA / chunk=1000 / thread=4      | 22.9초 | −15% (스케일 시 멀티스레드 한계) |
| #5 | JDBC / chunk=1000 / thread=1     | **6.2초** | **77% 처리 시간 감소**      |
| #6 | JDBC / chunk=1000 / thread=4     | **2.9초** | **89% 처리 시간 감소**      |

JDBC Bulk insert / 멀티스레드 조합이 가장 안정적이며, INSERT 방식 자체가 진짜 병목임을 정량적으로 확인했다.

---

## 로컬 실행

### 사전 조건

**MySQL 8**

```sql
CREATE DATABASE `goods-wms`;
CREATE USER 'app'@'localhost' IDENTIFIED BY 'app1234';
GRANT ALL PRIVILEGES ON `goods-wms`.* TO 'app'@'localhost';
FLUSH PRIVILEGES;
```

**Redis 7**

```bash
brew install redis
brew services start redis
```

### 실행

```bash
./gradlew bootRun
```

### Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

### 대량 입고 벤치마크

```bash
./scripts/generate-bulk-inbound-csv.sh 100000 ~/Desktop/bulk-inbound-100k.csv
./scripts/run-bulk-inbound-benchmark.sh ~/Desktop/bulk-inbound-100k.csv
```

---

## 진행 현황

### 완료
- [x] 3-tier + DDD 패키지 구조 설계
- [x] 도메인 모델 구현 (6개 바운디드 컨텍스트)
- [x] Application 레이어 (Command/Result 분리)
- [x] Repository 레이어 (Domain ↔ JpaEntity 매핑)
- [x] Presentation 레이어 (REST API + `@RestControllerAdvice`)
- [x] 동시성 제어 (낙관적 락 + 비관적 락)
- [x] Redis 기반 Cache-aside 적용 및 선택적 무효화
- [x] Java record 직렬화 호환 (`RecordSupportingTypeResolver`)
- [x] Spring Batch 기반 대량 입고 일괄 처리 + 성능 측정
- [x] 도메인 단위 테스트 (Inbound / Outbound / Inventory)
- [x] 비즈니스 규칙 위반에 대한 커스텀 도메인 예외
- [x] Swagger UI (springdoc-openapi)
