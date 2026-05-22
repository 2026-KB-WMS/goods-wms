# Goods WMS

캐릭터 굿즈 및 한정판 굿즈를 위한 재고 관리 시스템(Warehouse Management System)

---

## 프로젝트 목적

단순 수량 기록이 아닌, **누가 / 어느 창고에 / 무슨 상품을 / 얼마나 / 왜 입출고했는지**를 추적하고 재고 정합성을 안정적으로 유지하는 굿즈 도메인 특화 재고 관리 시스템

---

## 기술 스택

- Java 21
- Spring Boot 3.5
- Spring Data JPA
- MySQL 8
- Lombok

---

## 아키텍처

**3-tier + DDD(Domain-Driven Design)**

```
controller/     HTTP 요청/응답, Controller, DTO
business/
  application/    유스케이스 조합, 트랜잭션 관리
  domain/         핵심 비즈니스 규칙, 엔티티, 애그리거트, Repository 인터페이스
repository/             JPA Entity, Repository 구현체, Domain ↔ Persistence 매핑
```

---

## 바운디드 컨텍스트 & 도메인 모델

### Member
내부 운영 인력 관리

| 클래스 | 역할 |
|---|---|
| `Member` | 애그리거트 루트. 역할/상태 관리, 업무 수행 가능 여부 검증 |
| `MemberRole` | `ADMIN`, `WAREHOUSE_MANAGER`, `INBOUND_HANDLER`, `OUTBOUND_HANDLER` |
| `MemberStatus` | `ACTIVE`, `INACTIVE` |

### Product
굿즈 상품 식별 및 카탈로그 관리

| 클래스 | 역할 |
|---|---|
| `Product` | 애그리거트 루트. SKU, 에디션, 한정판 여부, 상품 유형 관리 |
| `ProductType` | `ACRYLIC_STAND`, `KEYRING`, `FIGURE`, `PHOTO_CARD`, `PLUSH`, `RANDOM_BOX`, `LIMITED_PACKAGE`, `OTHER` |
| `ProductStatus` | `ACTIVE`, `INACTIVE` |

### Warehouse
창고 및 보관 구역 관리

| 클래스 | 역할 |
|---|---|
| `Warehouse` | 애그리거트 루트. 창고 활성/비활성, 구역 추가, 구역 코드 중복 방지 |
| `WarehouseZone` | 창고 내 보관 구역 (Child Entity) |
| `ZoneType` | `NORMAL`, `DAMAGED`, `SECURE` |

### Inbound
입고 문서 및 검수 흐름 관리

| 클래스 | 역할 |
|---|---|
| `Inbound` | 애그리거트 루트. 입고 문서 생성, 상태 전이 관리 |
| `InboundLine` | 상품별 입고 라인, 검수 결과(정상/파손/검수대기) 기록 (Child Entity) |
| `InboundStatus` | `CREATED` → `INSPECTING` → `COMPLETED` / `CANCELED` |

### Outbound
출고 요청 및 완료 흐름 관리

| 클래스 | 역할 |
|---|---|
| `Outbound` | 애그리거트 루트. 출고 문서 생성, 상태 전이 관리 |
| `OutboundLine` | 상품별 출고 수량 (Child Entity) |
| `OutboundStatus` | `CREATED` → `VALIDATED` → `COMPLETED` / `CANCELED` |
| `OutboundPurpose` | `POPUP_STORE`, `EVENT_BOOTH`, `ONLINE_SALES`, `EXCHANGE`, `RETURN`, `DISPOSAL` |

### Inventory
창고/구역/상품 기준 현재 재고 상태 관리

| 클래스 | 역할 |
|---|---|
| `Inventory` | 애그리거트 루트. 상태별 재고 수량 관리, 가용 재고 계산, 음수 재고 방지 |
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

---

## 로컬 실행

### 사전 조건

MySQL 8이 실행 중이어야 하며, 아래 DB/계정이 생성되어 있어야 합니다.

```sql
CREATE DATABASE `goods-wms`;
CREATE USER 'app'@'localhost' IDENTIFIED BY 'app1234';
GRANT ALL PRIVILEGES ON `goods-wms`.* TO 'app'@'localhost';
FLUSH PRIVILEGES;
```

### 실행

```bash
./gradlew bootRun
```

---

## 진행 현황

- [x] 프로젝트 초기 설정 (Spring Boot, MySQL 연동)
- [x] 3-tier + DDD 패키지 구조 설계
- [x] 도메인 모델 구현 (6개 바운디드 컨텍스트)
- [x] Application 레이어 (유스케이스 서비스)
- [x] Data 레이어 (JPA Entity, Repository 구현체)
- [ ] Presentation 레이어 (Controller, DTO)
