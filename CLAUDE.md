# Goods WMS 작업 규칙

## 프로젝트
- 굿즈 창고 관리 시스템. Java 21, Spring Boot 3.5, Spring Batch, Spring Data JPA, MySQL 9.4 (로컬은 Docker, localhost:3307), Redis 7.
- 테스트는 H2로 돈다(`src/test/resources/application.properties`).
- `src/main/resources/application.properties`는 민감한 설정이라 .gitignore 대상이다. 내용을 출력하거나 커밋하지 않는다.

## 명령
- 전체 테스트: `./gradlew test`
- 한 클래스만: `./gradlew test --tests '패키지.클래스명'`
- 로컬 실행: `./gradlew bootRun` (로컬 MySQL과 Redis가 떠 있어야 한다)
- CSV 생성: `./scripts/generate-bulk-inbound-csv.sh <줄 수> <출력 파일>`
- 벤치마크: `./scripts/run-bulk-inbound-benchmark.sh <CSV 경로>`. 실행할 때마다 `inbound_lines`, `inbounds`를 지운다. 직접 실행하지 말고 명령만 알려 준다.

## 구조
- `business/domain`: 도메인 모델과 규칙 (JPA에 의존하지 않음)
- `business/application`: 유스케이스 서비스 (예: `InboundService`)
- `repository`: JPA 엔티티와 리포지토리
- `controller`: REST API. 응답은 `ApiResponse`, 예외는 `GlobalExceptionHandler`
- `batch/bulkinbound`: 대량 입고 Job (Reader → Processor → JPA/JDBC Writer)
- `batch/reconciliation`: 재고 정합성 검증 Job (매일 03:00, 수동 실행 API도 있음)
- `config`: Redis 캐시, 커밋 후 캐시 무효화(`InventoryCacheEvictor`)

## 지켜야 할 도메인 규칙
- 입고 상태: CREATED, INSPECTING, COMPLETED, CANCELED. 출고 상태: CREATED, VALIDATED, COMPLETED, CANCELED.
- 일반 입고 완료(`InboundService.completeInbound`)는 라인에 구역을 배정하고, 재고를 FOR UPDATE로 잠가 반영하고, 커밋 후 캐시를 지운다.
- 재고 수량 변경은 비관적 락, 입출고 문서 상태 변경은 `@Version` 낙관적 락을 쓴다.
- 정합성 배치는 구역이 일치하는 COMPLETED 라인만 합산한다.

## 작업 방식
1. 코드를 고치기 전에 계획을 먼저 보여 준다: 바꿀 파일, 영향받는 곳(다른 컨트롤러, 스케줄러, 스크립트, README, 테스트), 테스트 계획.
2. 설계 선택지가 있으면 장단점과 영향 범위를 표로 정리하고, 내가 고를 때까지 구현하지 않는다.
3. 문제를 재현하는 테스트를 먼저 만들고, 실패하는 것을 확인한 뒤 고친다.
4. 실험 범위 밖의 리팩터링, 이름 변경, 포맷 변경은 하지 않는다. 기존 테스트를 지우거나 약하게 바꾸지 않는다.
5. 끝나면 `./gradlew test` 전체 결과(통과·실패 수)를 보고한다.
6. 모르는 API나 설정은 추측하지 않는다. Spring Boot 3.5와 Spring Batch 5.2 공식 문서 기준으로 설명하고, 확인하지 못했으면 그렇다고 말한다.
7. 숫자는 내가 실행해서 알려 준 측정값만 쓴다. 측정 조건(건수, writer, chunk, thread, DB, 반복 횟수)을 함께 적는다.

## Git
- 커밋 메시지는 기존 형식을 따른다: `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:` + 한국어 요약.
- 커밋, push, 브랜치 삭제는 내가 확인한 뒤에 한다.

## 실험 기록
- 실험마다 `docs/experiments/NN-이름.md`를 `docs/experiments/TEMPLATE.md` 형식으로 쓴다.
- 'AI 사용 기록'에는 AI가 제안했다가 바뀐 것, AI가 틀린 것, 내가 결정한 것을 사실대로 적는다.