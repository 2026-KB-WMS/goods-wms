# 실험 1: async bulk inbound

- 날짜: 2026-11-12
- 브랜치 / PR:

## 질문

업로드 요청이 배치가 끝날 때까지 기다리지 않게 할 수 있나?

## 지금 동작

- 관련 코드:
  - `src/main/java/.../controller/BulkInboundController.java:51` — `POST /inbounds/bulk`가 `jobLauncher.run(bulkInboundJob, params)`를 **동기** 호출해 Job 완료까지 HTTP 응답이 지연된다.
  - `src/main/java/.../controller/BulkInboundController.java:63` — `GET /inbounds/bulk/jobs/{executionId}`는 `JobExplorer`로 상태·`durationMs` 조회.
- 재현 방법:
  - 로컬 MySQL(`jdbc:mysql://localhost:3307/goods_wms`), `writerType=jdbc`, `chunkSize=1000`, 10만 건.
  - `threadCount=1`과 `threadCount=4` 각각 3회씩, 2026-11-12 측정.
  - 업로드 응답 시간: `curl -w '%{time_total}'`로 POST 소요 측정.
  - Job 처리 시간: 응답의 `executionId`로 `GET /inbounds/bulk/jobs/{id}` 호출하여 바디의 `durationMs`.
  - **업로드 응답 시간이 Job `durationMs`보다 24~222ms 더 길다** → 요청이 Job 종료까지 열려 있다는 근거(§측정 참조).

## 선택지와 결정

여섯 하위 질문으로 쪼개서 각각 결정했다.

- **비동기 경계**: bulk 전용 async `JobLauncher` 빈 추가. 이름 매칭(`asyncBulkJobLauncher`)으로 주입해 `ReconciliationController`/`ReconciliationScheduler`가 받는 자동 설정 `jobLauncher`와 격리. `@Qualifier`/`@Primary`/`@BatchTaskExecutor` 전부 쓰지 않음(Lombok `@RequiredArgsConstructor`는 `copyableAnnotations` 설정 없이는 `@Qualifier`를 생성자 파라미터에 복사하지 않아 조용히 기본 빈이 주입될 수 있다). `@Async` 래퍼 쪽은 `JobLauncher.run()`이 `jobExecution`을 반환하는 단일 지점과 맞지 않아 어색해서 안 고름.
- **Reconciliation 범위**: 손대지 않음. 코드상 "동기여야 할 근거"는 없었지만(`ReconciliationScheduler`는 run 뒤 로그만, `ReconciliationController`는 이미 `HttpStatus.ACCEPTED` 반환) 실험 1은 bulk 업로드 블로킹만 다룸.
- **스레드 풀**: `ThreadPoolTaskExecutor` core=1, max=1, queueCapacity=1, `AbortPolicy`. 대량 CSV + 긴 트랜잭션이라 동시 실행 자체가 리스크이고, "즉시 202" 보장은 submit 성공만 필요해서 큐를 거의 안 둠.
- **거절 시 응답**: `Semaphore(1)` + `TaskDecorator`의 Runnable 전체를 감싼 finally에서만 release. `TaskExecutorJobLauncher`가 `TaskRejectedException`을 내부에서 삼키고 `jobExecution`을 FAILED로 마킹해 반환한다는 소스를 확인한 뒤(`spring-batch-core 5.2.5`), 풀 거절은 반환값의 `status == FAILED && startTime == null`로 결정적 판별. 전용 `BulkInboundBusyException` + `GlobalExceptionHandler`에서 `Retry-After: 5` 헤더 포함한 `ErrorResponse`(새 `ErrorCode.BULK_INBOUND_BUSY`) 반환.
- **재시작 후 STARTED 정리**: `ApplicationRunner`에서 빈 생성 시각(`startupMarker`)보다 `createTime`이 이전인 `bulkInboundJob` 실행을 `ABANDONED`로 마킹. `exitStatus`는 status와 맞춰 `new ExitStatus("ABANDONED", "Server restart")`. `SimpleJobOperator.abandon()`은 `STARTING`/`STARTED` 상태에서는 `JobExecutionAlreadyRunningException`을 던지므로(소스 확인) 그걸 쓰지 않고 `JobRepository.update`로 상태를 직접 업그레이드.
- **테스트 방식**: 컨트롤러 테스트는 `@SpringBootTest + @AutoConfigureMockMvc + @MockitoSpyBean BulkInboundProcessor`에 `doAnswer(latch.await)`로 Job을 중간에 멈춤. `@TestConfiguration`으로 같은 빈을 재정의하면 `allow-bean-definition-overriding` 없이는 `BeanDefinitionOverrideException`이 나므로 피했다. `@SpringBatchTest`는 Job/Launcher 둘 다 2개라 `BatchTestContextBeanPostProcessor`의 `ifUnique` 조건을 못 넘어 미사용 — 대신 `new JobRepositoryTestUtils(jobRepository)`만 생성해 메타 테이블 정리. `MockMvcResultMatchers.request().asyncStarted()`는 `Callable`/`DeferredResult` 반환일 때만 true라 이번 설계에 안 맞아 **Future.get 타임아웃**으로 "컨트롤러가 기한 내 응답했는가"를 판정.

## 바꾼 것

운영 코드

- `src/main/java/.../batch/bulkinbound/AsyncBulkJobLauncherConfig.java` (신규) — `Semaphore(1)` 빈, `ThreadPoolTaskExecutor`(core=1, max=1, queue=1, `AbortPolicy`, `TaskDecorator`가 Runnable 전체를 try-finally로 감싸서 `bulkInboundSemaphore.release()`), `TaskExecutorJobLauncher`를 `asyncBulkJobLauncher` 이름으로 등록. `initialize()` 명시 호출 안 함(빈 생명주기가 `afterPropertiesSet` 처리).
- `src/main/java/.../controller/bulkinbound/BulkInboundController.java` — 필드 `JobLauncher jobLauncher` → `JobLauncher asyncBulkJobLauncher` + `Semaphore bulkInboundSemaphore`(둘 다 이름 매칭 주입). 메서드 흐름: `file.isEmpty()` 검사 → `bulkInboundSemaphore.tryAcquire()` 실패면 `BulkInboundBusyException` → 임시 파일 저장 → `asyncBulkJobLauncher.run()` → 반환값이 `FAILED && startTime == null`이면 release 후 `BulkInboundBusyException` → `submitted = true` → try 밖에서 응답 생성(`202 Accepted` + `BulkInboundUploadResponse`). `catch (Exception e)`에서 `!submitted`일 때만 release(체크드 예외 포함).
- `src/main/java/.../controller/bulkinbound/BulkInboundBusyException.java` (신규) — `RuntimeException` 서브클래스.
- `src/main/java/.../common/ErrorCode.java` — `BULK_INBOUND_BUSY(SERVICE_UNAVAILABLE, ...)` 엔트리.
- `src/main/java/.../controller/common/GlobalExceptionHandler.java` — `BulkInboundBusyException` 전용 `@ExceptionHandler`가 `ErrorResponse` + `Retry-After: 5` 헤더 반환.
- `src/main/java/.../batch/bulkinbound/BulkInboundStaleJobCleanupRunner.java` (신규) — `ApplicationRunner`. `startupMarker = LocalDateTime.now()` 필드 초기화 시점 고정, `jobExplorer.findRunningJobExecutions("bulkInboundJob")` 조회 → `createTime < startupMarker`만 `ABANDONED`로 업그레이드 + `endTime = now` + `exitStatus = new ExitStatus("ABANDONED", "Server restart")` + `jobRepository.update(execution)`.

테스트

- `build.gradle` — `testImplementation 'org.springframework.batch:spring-batch-test'` 1줄.
- `src/test/resources/application.properties` — `spring.batch.job.enabled=false` 1줄(기존 Job 2개 환경에서 테스트 컨텍스트 로드 깨짐 복구). 커밋 `1fedfbe`에 이미 포함.
- `src/test/java/.../controller/bulkinbound/AsyncBulkInboundControllerTest.java` (신규) — 4개 테스트. `@MockitoSpyBean BulkInboundProcessor`에 `doAnswer(latch.await)`, `@MockitoSpyBean(name = "asyncBulkJobLauncher") JobLauncher`. `@AfterEach`에서 latch 해제 → `jobExplorer.findRunningJobExecutions("bulkInboundJob")` 빔 대기 → 테스트가 띄운 Future 완료 대기 → `ExecutorService` shutdown → DELETE child→parent → `JobRepositoryTestUtils.removeJobExecutions()`.
- `src/test/java/.../batch/bulkinbound/BulkInboundStaleJobCleanupRunnerTest.java` (신규) — Runner를 테스트 내부에서 수동 생성해 `startupMarker` 고정, stale/fresh 두 `JobExecution`을 생성 후 `runner.run()` 호출, 상태·`exitStatus`·`endTime` 검증.

스크립트 · 문서

- `scripts/run-bulk-inbound-benchmark.sh` 전면 재작성 — `DB_USER`/`DB_PASS`/`DB_NAME`/`DB_HOST`/`DB_PORT` 환경변수 필수(비면 종료), 모든 `mysql` 호출에 `-h/-P/--protocol=TCP`, 시작 시 `SELECT @@port, VERSION(), DATABASE()` 출력, 매 케이스 cleanup 후 `inbounds`/`inbound_lines` 행 수 0 확인, `COMPLETED`/`FAILED`/`STOPPED`/`ABANDONED`까지 폴링, 503 수신 시 `Retry-After` 대기 후 재시도, `CASES`/`REPS`/`POLL_INTERVAL`/`POLL_TIMEOUT` 인자화. 기존 `2>/dev/null`을 제거해 삭제 실패가 그대로 드러나게 함.
- `README.md` — 엔드포인트 테이블에 202/503/폴링 명세 보강, 벤치마크 사용법에 DB 환경변수 자리표시자 추가, `MySQL 8` 두 곳을 `MySQL 9.4 (로컬 확인 환경: Docker mysql:latest, 측정 시점 9.4.0, 포트 3307)`로.
- `CLAUDE.md` — 스택 표기를 `MySQL 9.4 (로컬은 Docker, localhost:3307)`로.
- `docs/experiments/01-async-bulk-inbound.md` (이 파일, 신규).

## 검증

| 테스트 | 무엇을 확인하나 | 고치기 전에 실패했나 |
|---|---|---|
| `AsyncBulkInboundControllerTest.업로드하면_즉시_202와_executionId를_받고_Job은_아직_실행중이다` | 컨트롤러가 Job 완료를 기다리지 않고 즉시 202를 반환 + Job이 STARTING/STARTED 상태 | 예 — 동기 코드에서 `future.get(2s)`가 `TimeoutException`으로 실패 |
| `AsyncBulkInboundControllerTest.첫_Job이_블로킹된_상태에서_두번째_업로드는_503과_Retry_After를_받는다` | 포화 시 즉시 503 + `Retry-After` 반환 | 예 — 동기 코드에서 두 번째 `future.get(1s)`가 `TimeoutException`으로 실패 |
| `AsyncBulkInboundControllerTest.첫_Job이_COMPLETED된_뒤의_업로드는_202를_받는다` | 완료 뒤 permit이 돌아와 다음 업로드가 202. COMPLETED 직후의 짧은 release 지연은 재시도로 흡수 | 아니오 — 동기 코드에서도 통과(회귀 방지용). 첫 Job이 COMPLETED인지, 재시도로 받은 executionId가 첫 것과 다른지도 함께 검증 |
| `AsyncBulkInboundControllerTest.run이_checked_exception을_던지면_permit이_누수되지_않는다` | `JobLauncher.run()`의 체크드 예외도 catch로 잡혀 release되고 다음 업로드가 202 | 예 — 좁은 `catch (RuntimeException \| IOException)` 상태에서 두 번째 업로드가 "expected 202 but was 503"로 실패. `catch (Exception)`으로 복원 후 통과 |
| `BulkInboundStaleJobCleanupRunnerTest.기동_이전에_생성된_STARTED_실행은_ABANDONED가_되고_이후_생성은_그대로다` | `startupMarker` 이전 생성 STARTED만 ABANDONED로 정리, 이후 생성은 유지 | 예 — `run()` 본문을 빈 상태로 둔 뒤 "expected ABANDONED but was STARTED"로 실패. 구현 복원 후 통과 |

전체 테스트: 통과 110건 / 실패 0건

## 측정

- 조건: 10만 건, `writerType=jdbc`, `chunkSize=1000`, Docker `mysql:latest` (9.4.0), `localhost:3307`, macOS 로컬 JVM, 2026-11-12 측정, `threadCount=1`과 `threadCount=4` 각 3회.
  - 바꾸기 전: 임시 스크립트로 thread=1 세 번 → thread=4 세 번 순서로 측정.
  - 바꾼 뒤: 새 벤치마크 스크립트(§7.7)로 thread=1과 thread=4를 번갈아 실행, 매 케이스 cleanup 후 `inbounds=0 inbound_lines=0` 확인.
  - 두 측정 모두 업로드 응답은 `curl -w '%{time_total}'`, Job 처리는 `GET /inbounds/bulk/jobs/{id}`의 `durationMs`. 같은 DB.

| 항목 | 바꾸기 전 | 바꾼 뒤 |
|---|---|---|
| thread=1 · 업로드 응답(ms) | run1 16,216 / run2 16,719 / run3 16,619 (평균 16,518) | run1 121 / run2 24 / run3 35 (평균 60) |
| thread=1 · Job `durationMs` | run1 15,994 / run2 16,685 / run3 16,586 (평균 16,422) | run1 16,828 / run2 16,148 / run3 16,275 (평균 16,417) |
| thread=1 · 업로드 − Job(ms) | 222 / 34 / 33 (min 33, max 222) | 해당 없음(비동기: 업로드가 Job을 기다리지 않음) |
| thread=1 · Job 상태 | 3회 모두 COMPLETED | 3회 모두 COMPLETED |
| thread=4 · 업로드 응답(ms) | run1 6,540 / run2 6,521 / run3 6,550 (평균 6,537) | run1 26 / run2 25 / run3 24 (평균 25) |
| thread=4 · Job `durationMs` | run1 6,516 / run2 6,490 / run3 6,520 (평균 6,509) | run1 6,674 / run2 6,391 / run3 6,306 (평균 6,457) |
| thread=4 · 업로드 − Job(ms) | 24 / 31 / 30 (min 24, max 31) | 해당 없음(비동기: 업로드가 Job을 기다리지 않음) |
| thread=4 · Job 상태 | 3회 모두 COMPLETED | 3회 모두 COMPLETED |

표 아래 참고: thread=1 바꾼 뒤 run1의 121ms는 다른 회차(24ms, 35ms)보다 눈에 띄게 크지만, 원인은 이번 실험에서 확인하지 않았다.

결론 한 줄: 업로드 응답이 Job 처리 시간(약 16.4초 / 6.5초)에서 수십 ms로 줄었고, Job 처리 시간은 반복 편차 범위 안에서 같았다.

## 영향 범위

- 함께 바꾼 곳: `BulkInboundController`, 신규 `AsyncBulkJobLauncherConfig`, 신규 `BulkInboundBusyException`, `ErrorCode`, `GlobalExceptionHandler`, 신규 `BulkInboundStaleJobCleanupRunner`, `scripts/run-bulk-inbound-benchmark.sh`, `README.md`, `CLAUDE.md`, 신규 `AsyncBulkInboundControllerTest`, 신규 `BulkInboundStaleJobCleanupRunnerTest`, `build.gradle`(테스트 의존성), `src/test/resources/application.properties`(기준선 복구).
- 바꾸지 않았지만 영향을 받을 수 있는 곳:
  - `ReconciliationController` — 응답 코드는 202 명세인데 실제 실행은 동기. 이름 매칭 주입이라 기존 자동 설정 `jobLauncher`를 그대로 받는다.
  - `ReconciliationScheduler` — 매일 03:00 `inventoryReconciliationJob` 동기 실행. 그대로.
  - 자동 설정 `JobOperator` — 생성자 파라미터 이름이 `jobLauncher`라 역시 기존 동기 빈 주입.
  - `src/main/resources/application.properties`(.gitignore) — 로컬 DB 접속 설정. Docker `mysql:latest` 포트/계정과 맞아야 서버가 뜸.

## 남은 한계

- `ReconciliationController`는 응답 코드만 202이고 실제 실행은 동기. 별도 실험으로 뗀다.
- 재시작 복구는 **서버 1대 전제**. 멀티 노드로 늘리면 다른 노드가 돌리고 있는 실행도 ABANDONED로 잘못 마킹할 수 있다.
- 재시작 복구는 Job 레벨만. `BATCH_STEP_EXECUTION`의 STARTED는 손대지 않았다 — 재시작 가능 정책은 **실험 2(임시 파일 처리 — 재시작 시 파일 필요 여부)** / **실험 4(중복 업로드 — 멀티스레드 Step 재시작 안전성)**에서 다시 본다.
- `Semaphore` permit=1 — 단일 노드 기준 동시 1건. 처리량 확장 요구가 생기면 §6.3 (b) 쪽으로 늘린다.
- **COMPLETED 직후의 업로드는 잠깐 503을 받을 수 있다**. Job의 COMPLETED DB 기록 이후에도 launcher 완료 로그와 워커 반환이 끝나야 TaskDecorator finally에서 permit이 반환된다(수 ms~수십 ms 창). 클라이언트는 `Retry-After`로 재시도.
- `mysql:latest`는 다시 받으면 버전이 바뀔 수 있어, 재측정 때는 버전을 다시 확인한다.
- 기준선 복구 커밋 `1fedfbe(test:)`에 `CLAUDE.md`와 `docs/experiments/TEMPLATE.md`가 섞여 push됨. force push 없이 그대로 둠.

## AI 사용 기록

- 맡긴 일: Claude Code(이 세션)에 비동기 전환 설계·구현, TDD 테스트 작성, 벤치마크 스크립트 수정, 실험 기록 초안을 맡겼다. 별도 AI 리뷰(채팅 Claude)에게 설계 리뷰를 받았다.
- AI가 틀렸거나 내가 바꾼 것:
  - **주입 방식**: Claude Code 초안이 `@Qualifier` + `@RequiredArgsConstructor` 조합을 전제. 별도 AI 리뷰에서 Lombok의 `copyableAnnotations`가 이 저장소의 `lombok.config`에 없어서 `@Qualifier`가 생성자 파라미터로 복사되지 않는다는 지적을 받았다. `lombok.config` 부재를 확인하고 **빈 이름 = 필드 이름 매칭**으로 반영.
  - **가상 스레드 설명**: Claude Code 초안이 "Spring Batch에 가상 스레드 공식 문서 지지 없음"으로 썼는데, 리뷰에서 Spring Batch 5.1 What's New에 명시돼 있다는 지적을 받았다. (d)를 안 고르는 이유는 "지원 여부"가 아니라 "실험 목적(동시 1건 상한)"이었음을 바로잡았다.
  - **permit release 설계**: Claude Code 초안이 "try-finally + afterJob listener 둘 다 release"였다. 리뷰에서 (a) 비동기 `run()`은 접수 직후 반환하므로 호출 스레드 finally가 Job 실행 중 release하고 afterJob이 중복 release, (b) afterJob은 `AbstractJob#execute`의 finally에서 **마지막 `jobRepository.update`보다 먼저** 불려서 큐 0이면 permit을 얻은 다음 요청이 풀에서 거절되고 `TaskExecutorJobLauncher`가 그걸 삼켜 FAILED를 돌려준 채 202가 나갈 수 있음을 지적. 소스(`TaskExecutorJobLauncher`, `AbstractJob`, `SimpleJobOperator`)로 확인 후 **release는 TaskDecorator의 Runnable 전체를 감싼 finally 한 곳에서만**, 호출 스레드에서는 **워커 진입 실패 경로**(파일 저장 실패, run 예외, `status==FAILED && startTime==null`)만 release로 반영.
  - **테스트 정리 방식**: Claude Code 초안이 `TRUNCATE`를 썼는데 리뷰에서 H2는 FK 참조받는 테이블을 TRUNCATE 못 함을 지적. DELETE 자식→부모 순으로 수정. `@SpringBatchTest`의 `BatchTestContextBeanPostProcessor`가 `setJob`/`setJobLauncher`를 `ifUnique`로 주입한다는 점도 초안에서 빠져 있어, Job/Launcher 둘 다 2개인 이 저장소에선 둘 다 주입되지 않음을 반영해 `@SpringBatchTest` 없이 `new JobRepositoryTestUtils(jobRepository)`만 쓰도록 바꿨다.
  - **catch 범위 — 리뷰가 먼저 틀리게 짚었다가 바로잡은 사안**: Claude Code가 처음 쓴 catch는 `catch (RuntimeException | IOException e)`였다. 별도 AI 리뷰가 처음엔 "**RuntimeException** 누수"로 잘못 짚었고, 내가 Claude Code 초안의 catch 범위를 리뷰에 보여 주자 실제 누수 경로는 `JobLauncher.run()`이 선언하는 **체크드 예외**(`JobParametersInvalidException` 등)임을 리뷰가 바로잡았다. 리뷰 원래 테스트 제안(`doThrow(new RuntimeException(...))`)은 **수정 전 catch에서도 이미 걸려 통과**하는 테스트라, `doThrow(new JobParametersInvalidException(...))`로 바꿔 좁은 catch에서 "expected 202 but was 503"가 재현되는 것을 확인한 뒤 `catch (Exception e)`로 수정했다.
  - **벤치마크 DB 접속 — 리뷰가 원인을 잘못 짚은 사안**: 새 스크립트가 `mysql` 호출에 호스트/포트 없이 돌아 ERROR 2002로 실패했을 때, 리뷰가 원인을 "**로컬 MySQL 고장**(앱 DB가 아닌 다른 MySQL에 cleanup이 실행돼 데이터가 쌓임)"으로 잘못 짚었다. 그 추측을 따라 **나는 앱과 무관한 Homebrew MySQL의 데이터 폴더를 9.2→9.7로 업그레이드**했다(사전 백업함. 그 폴더엔 `goods_wms` DB도 `app` 계정도 없어 실험엔 영향 없음. 다만 다른 프로젝트용 데이터 폴더가 9.7로 올라가 되돌릴 수 없음). 이전 측정에 쓴 임시 스크립트에는 `-h/-P`가 있었던 것을 내가 직접 확인해 **실제 원인은 새 스크립트에 `DB_HOST`/`DB_PORT`가 없어 기본 소켓(`/tmp/mysql.sock`)으로 간 것**임을 찾았다. 반영: `DB_HOST`/`DB_PORT` 필수화 + 모든 `mysql` 호출에 `-h/-P/--protocol=TCP` + 시작 시 `SELECT @@port, VERSION(), DATABASE()` + cleanup 후 행 수 체크. "바꾸기 전" 측정값은 임시 스크립트가 TCP로 앱 DB에 붙어 잰 것이라 유효해서 그대로 남겼다.
- 검증 방법:
  - 소스 확인: Claude Code와 별도 AI 리뷰(`TaskExecutorJobLauncher.run`의 `TaskRejectedException` catch, `SimpleJobOperator.abandon`의 상태 가드, `AbstractJob.execute` 흐름 — `spring-batch-core 5.2.5`).
  - TDD: 각 핵심 변경마다 **고치기 전 상태에서 테스트가 어떻게 실패하는지**를 먼저 보여준 뒤 구현/수정 → 통과.
  - permit 누수는 `catch (RuntimeException | IOException)`로 임시 되돌려 "expected 202 but was 503" 재현, 복원 후 통과.
  - Runner는 `run()` 본문을 빈 상태로 임시 되돌려 "expected ABANDONED but was STARTED" 재현, 복원 후 통과.
  - 벤치마크는 접속한 서버(`@@port`, `VERSION`, `DATABASE`)와 cleanup 후 행 수를 출력해 확인할 수 있게 함.
- 내가 직접 결정한 것:
  - 각 하위 질문(§6.1~§6.6)의 최종 선택.
  - Reconciliation은 이번 실험 범위 밖으로 유지(코드상 근거 없음에도 격리 우선).
  - 재시작 복구의 상태 코드를 `FAILED`가 아닌 `ABANDONED`로 통일(멀티스레드 Step 재시작 가능성 보장 안 됨 근거). (별도 AI 리뷰 제안 → 채택)
  - `startupMarker`를 Runner 빈 생성 시각으로 둬 서블릿 ready 이후 들어온 신규 업로드를 보호. (별도 AI 리뷰 제안 → 채택)
  - `Semaphore`/`ThreadPoolTaskExecutor` 수치: core=max=1, queue=1, permit=1. TaskDecorator finally에서만 release. (별도 AI 리뷰 제안 → 채택)
  - MySQL 표기(안 1+2 합침 — "MySQL 9.4 (로컬 확인 환경: Docker mysql:latest, 측정 시점 9.4.0, 포트 3307)").
  - 결론 한 줄 문구 확정.

## 배운 것

1.
2.
3.
