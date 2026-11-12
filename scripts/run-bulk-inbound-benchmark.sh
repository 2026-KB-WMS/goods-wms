#!/usr/bin/env bash
# 대량 입고 성능 측정 벤치마크 (비동기 접수 + 폴링)
#
# 각 케이스 실행 전 inbounds / inbound_lines 데이터를 지운다.
# 서버가 ${BASE_URL}에서 실행 중이어야 한다.
#
# 필수 환경변수:
#   DB_USER, DB_PASS, DB_NAME, DB_HOST, DB_PORT
#   mysql 호출은 모두 -h/-P/--protocol=TCP로 한다. 안 그러면 CLI가 기본 소켓
#   (/tmp/mysql.sock)으로 가서 앱 DB와 다른 로컬 MySQL에 붙거나 ERROR 2002로 실패한다.
#
# 선택 환경변수:
#   BASE_URL   기본 http://localhost:8080
#   CASES      쉼표 구분 "writer:chunk:thread" 리스트.
#              비면 기본 6개 매트릭스:
#                jpa:1000:1, jpa:100:1, jpa:10000:1,
#                jpa:1000:4, jdbc:1000:1, jdbc:1000:4
#
# 위치 인자:
#   $1 CSV_PATH         기본 /Users/byeonguk/Desktop/bulk-inbound-10k.csv
#   $2 REPS             반복 횟수, 기본 1
#   $3 POLL_INTERVAL    상태 조회 간격(초), 기본 1
#   $4 POLL_TIMEOUT     종료 상태 대기 상한(초), 기본 300
#
# 예시:
#   DB_USER=<앱 DB 사용자> DB_PASS=<비밀번호> DB_NAME=<DB 이름> \
#   DB_HOST=<호스트> DB_PORT=<포트> \
#     ./scripts/run-bulk-inbound-benchmark.sh ~/Desktop/bulk-100k.csv 3
#
#   DB_USER=<앱 DB 사용자> DB_PASS=<비밀번호> DB_NAME=<DB 이름> \
#   DB_HOST=<호스트> DB_PORT=<포트> \
#   CASES=jdbc:1000:1,jdbc:1000:4 \
#     ./scripts/run-bulk-inbound-benchmark.sh ~/Desktop/bulk-100k.csv 3 2 600

set -euo pipefail

: "${DB_USER:?DB_USER env is required}"
: "${DB_PASS:?DB_PASS env is required}"
: "${DB_NAME:?DB_NAME env is required}"
: "${DB_HOST:?DB_HOST env is required}"
: "${DB_PORT:?DB_PORT env is required}"

CSV_PATH="${1:-/Users/byeonguk/Desktop/bulk-inbound-10k.csv}"
REPS="${2:-1}"
POLL_INTERVAL="${3:-1}"
POLL_TIMEOUT="${4:-300}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

DEFAULT_CASES="jpa:1000:1,jpa:100:1,jpa:10000:1,jpa:1000:4,jdbc:1000:1,jdbc:1000:4"
CASES_LIST="${CASES:-$DEFAULT_CASES}"

if [ ! -f "$CSV_PATH" ]; then
    echo "CSV 파일을 찾을 수 없습니다: $CSV_PATH" >&2
    exit 1
fi

for tool in curl jq mysql; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "필수 도구가 설치되어 있지 않습니다: $tool" >&2
        exit 1
    fi
done

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

mysql_app() {
    mysql -h "$DB_HOST" -P "$DB_PORT" --protocol=TCP \
        -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" "$@"
}

cleanup_db() {
    # 삭제 실패(권한/연결/락)는 조용히 숨기지 않고 그대로 노출한다.
    mysql_app -e "DELETE FROM inbound_lines; DELETE FROM inbounds;"

    local rows_inbounds rows_lines
    rows_inbounds=$(mysql_app -N -B -e "SELECT COUNT(*) FROM inbounds;")
    rows_lines=$(mysql_app -N -B -e "SELECT COUNT(*) FROM inbound_lines;")
    echo "  cleanup 후 행 수: inbounds=${rows_inbounds} inbound_lines=${rows_lines}"
    if [ "$rows_inbounds" != "0" ] || [ "$rows_lines" != "0" ]; then
        echo "cleanup이 데이터를 비우지 못했다. 중단." >&2
        exit 1
    fi
}

# 종료 상태(COMPLETED/FAILED/STOPPED/ABANDONED) 중 하나가 될 때까지 폴링.
# 결과 바디는 $TMPDIR/detail.json 에 저장. 성공 시 status를 stdout에 echo.
poll_until_terminal() {
    local exec_id=$1
    local started=$SECONDS status
    while :; do
        curl -s "${BASE_URL}/inbounds/bulk/jobs/${exec_id}" -o "$TMPDIR/detail.json"
        status=$(jq -r .data.status "$TMPDIR/detail.json")
        case "$status" in
            COMPLETED|FAILED|STOPPED|ABANDONED)
                echo "$status"
                return 0
                ;;
        esac
        if (( SECONDS - started >= POLL_TIMEOUT )); then
            echo "TIMEOUT"
            return 1
        fi
        sleep "$POLL_INTERVAL"
    done
}

# 업로드 1회. 503이면 Retry-After만큼 대기 후 재시도. 202 수신 시 exec_id/upload_time 반환.
# stdout: "<exec_id> <upload_time_sec>"
submit_until_accepted() {
    local writer=$1 chunk=$2 thread=$3
    while :; do
        : > "$TMPDIR/resp-headers.txt"
        : > "$TMPDIR/resp-body.json"
        local http_code upload_time retry_after
        {
            http_code=$(curl -s -o "$TMPDIR/resp-body.json" -D "$TMPDIR/resp-headers.txt" \
                -w '%{http_code} %{time_total}' \
                -X POST \
                "${BASE_URL}/inbounds/bulk?writerType=${writer}&chunkSize=${chunk}&threadCount=${thread}" \
                -F "file=@${CSV_PATH}")
        }
        upload_time="${http_code#* }"
        http_code="${http_code%% *}"

        if [ "$http_code" = "503" ]; then
            retry_after=$(grep -i '^retry-after:' "$TMPDIR/resp-headers.txt" \
                | awk '{print $2}' | tr -d '\r' | head -1)
            retry_after="${retry_after:-5}"
            echo "  503 수신 (writer=$writer chunk=$chunk thread=$thread) — Retry-After=${retry_after}s 대기 후 재시도" >&2
            sleep "$retry_after"
            continue
        fi
        if [ "$http_code" != "202" ]; then
            echo "업로드 실패: http=${http_code} body=$(cat "$TMPDIR/resp-body.json")" >&2
            return 1
        fi

        local exec_id
        exec_id=$(jq -r .data.executionId "$TMPDIR/resp-body.json")
        echo "$exec_id $upload_time"
        return 0
    done
}

run_case() {
    local label=$1 writer=$2 chunk=$3 thread=$4 rep=$5
    cleanup_db

    local line exec_id upload_time status duration_ms
    line=$(submit_until_accepted "$writer" "$chunk" "$thread")
    exec_id="${line%% *}"
    upload_time="${line##* }"

    status=$(poll_until_terminal "$exec_id")
    duration_ms=$(jq -r .data.durationMs "$TMPDIR/detail.json")

    printf "%-26s | rep=%s writer=%-4s chunk=%-5s thread=%-2s | upload=%ss  job=%sms  status=%s\n" \
        "$label" "$rep" "$writer" "$chunk" "$thread" "$upload_time" "$duration_ms" "$status"
}

echo "=== 대량 입고 벤치마크 ==="
echo "CSV           : $CSV_PATH"
echo "REPS          : $REPS"
echo "POLL_INTERVAL : ${POLL_INTERVAL}s"
echo "POLL_TIMEOUT  : ${POLL_TIMEOUT}s"
echo "BASE_URL      : $BASE_URL"
echo "CASES         : $CASES_LIST"
echo ""
echo "--- 접속한 MySQL 확인 (${DB_HOST}:${DB_PORT}) ---"
mysql_app -t -e "SELECT @@port AS port, VERSION() AS version, DATABASE() AS db;"
echo ""

IFS=',' read -ra CASES_ARR <<< "$CASES_LIST"

for rep in $(seq 1 "$REPS"); do
    idx=0
    for case_spec in "${CASES_ARR[@]}"; do
        idx=$((idx + 1))
        IFS=':' read -r writer chunk thread <<< "$case_spec"
        run_case "#${idx} ${writer}/${chunk}/${thread}" "$writer" "$chunk" "$thread" "$rep"
    done
done

echo ""
echo "=== 측정 완료 ==="
