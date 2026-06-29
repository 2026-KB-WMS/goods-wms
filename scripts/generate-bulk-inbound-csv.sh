#!/usr/bin/env bash
# 대량 입고 테스트용 CSV 생성 스크립트
#
# Usage:
#   ./scripts/generate-bulk-inbound-csv.sh [라인수] [출력파일]
#
# 예시:
#   ./scripts/generate-bulk-inbound-csv.sh 100 /tmp/bulk-inbound-100.csv
#   ./scripts/generate-bulk-inbound-csv.sh 10000 /tmp/bulk-inbound-10k.csv
#   ./scripts/generate-bulk-inbound-csv.sh 100000 /tmp/bulk-inbound-100k.csv
#
# 생성 규칙:
#   - 창고 1, 멤버 1, 상품 1~2 사용 (사전 등록 필요)
#   - 5개 공급사로 분배 (정렬되어 같은 마스터가 연속 배치됨)
#   - 각 라인 수량은 1~100 랜덤, 전량 정상 처리

set -euo pipefail

LINES="${1:-100}"
OUTPUT="${2:-/tmp/bulk-inbound-${LINES}.csv}"

WAREHOUSE_ID=1
HANDLER_ID=1
SUPPLIERS=("공급사A" "공급사B" "공급사C" "공급사D" "공급사E")
PRODUCT_MAX=2  # 사전 등록된 상품 id 범위 (1 ~ PRODUCT_MAX)

PER_SUPPLIER=$((LINES / ${#SUPPLIERS[@]}))
REMAINDER=$((LINES % ${#SUPPLIERS[@]}))

{
    echo "warehouseId,handlerId,supplierName,productId,orderedQuantity,normalQuantity,damagedQuantity,pendingInspectionQuantity"
    for idx in "${!SUPPLIERS[@]}"; do
        supplier="${SUPPLIERS[$idx]}"
        count=$PER_SUPPLIER
        if [ "$idx" -eq 0 ]; then
            count=$((count + REMAINDER))
        fi
        for ((i=1; i<=count; i++)); do
            product=$((RANDOM % PRODUCT_MAX + 1))
            ordered=$((RANDOM % 100 + 1))
            echo "$WAREHOUSE_ID,$HANDLER_ID,$supplier,$product,$ordered,$ordered,0,0"
        done
    done
} > "$OUTPUT"

echo "생성 완료: $OUTPUT (${LINES} lines)"
