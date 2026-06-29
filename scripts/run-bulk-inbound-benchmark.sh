#!/usr/bin/env bash
# 대량 입고 성능 측정 벤치마크
#
# Usage:
#   ./scripts/run-bulk-inbound-benchmark.sh [CSV 경로]
#
# 예시:
#   ./scripts/run-bulk-inbound-benchmark.sh /Users/byeonguk/Desktop/bulk-inbound-10k.csv
#
# 매트릭스 (PR 본문 표와 동일):
#   #1 jpa  1000 1
#   #2 jpa  100  1
#   #3 jpa  10000 1
#   #4 jpa  1000 4
#   #5 jdbc 1000 1
#   #6 jdbc 1000 4
#
# 각 케이스 실행 전 inbounds / inbound_lines 데이터를 비웁니다.
# 서버가 http://localhost:8080 에서 실행 중이어야 합니다.

set -euo pipefail

CSV_PATH="${1:-/Users/byeonguk/Desktop/bulk-inbound-10k.csv}"
BASE_URL="http://localhost:8080"
DB_USER="app"
DB_PASS="app1234"
DB_NAME="goods-wms"

if [ ! -f "$CSV_PATH" ]; then
    echo "CSV 파일을 찾을 수 없습니다: $CSV_PATH" >&2
    exit 1
fi

cleanup() {
    mysql -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" \
        -e "DELETE FROM inbound_lines; DELETE FROM inbounds;" 2>/dev/null
}

run_case() {
    local label=$1 writer=$2 chunk=$3 thread=$4
    cleanup

    local resp
    resp=$(curl -s -X POST \
        "${BASE_URL}/inbounds/bulk?writerType=${writer}&chunkSize=${chunk}&threadCount=${thread}" \
        -F "file=@${CSV_PATH}")
    local exec_id
    exec_id=$(echo "$resp" | jq -r .data.executionId)

    local detail
    detail=$(curl -s "${BASE_URL}/inbounds/bulk/jobs/${exec_id}")
    local duration status read_count write_count
    duration=$(echo "$detail" | jq -r .data.durationMs)
    status=$(echo "$detail" | jq -r .data.status)
    read_count=$(echo "$detail" | jq -r .data.readCount)
    write_count=$(echo "$detail" | jq -r .data.writeCount)

    printf "%-22s | writer=%-4s chunk=%-5s thread=%-2s → %7sms  read=%s  write=%s  status=%s\n" \
        "$label" "$writer" "$chunk" "$thread" "$duration" "$read_count" "$write_count" "$status"
}

echo "=== 대량 입고 벤치마크 ==="
echo "CSV: $CSV_PATH"
echo ""

run_case "#1 베이스라인"   jpa  1000  1
run_case "#2 청크 100"     jpa  100   1
run_case "#3 청크 10000"   jpa  10000 1
run_case "#4 JPA 멀티(4)"  jpa  1000  4
run_case "#5 JDBC 단일"    jdbc 1000  1
run_case "#6 JDBC 멀티(4)" jdbc 1000  4

echo ""
echo "=== 측정 완료 ==="
