-- 입출고 라인에 zone_id 컬럼 추가 후 기존 데이터 백필
--
-- 사전 조건:
--   Spring Boot ddl-auto=update가 다음 컬럼을 이미 생성한 상태여야 합니다.
--     inbound_lines.normal_zone_id
--     inbound_lines.damaged_zone_id
--     outbound_lines.zone_id
--
-- 백필 정책:
--   - inbound_lines.normal_zone_id    → 해당 inbound의 warehouse NORMAL zone id
--   - inbound_lines.damaged_zone_id   → 해당 inbound의 warehouse DAMAGED zone id
--                                       (없으면 NORMAL zone id로 폴백)
--   - outbound_lines.zone_id          → 해당 outbound의 warehouse NORMAL zone id

-- ==============================
-- inbound_lines.normal_zone_id
-- ==============================
UPDATE inbound_lines il
JOIN inbounds i
    ON il.inbound_id = i.id
JOIN warehouse_zones nwz
    ON nwz.warehouse_id = i.warehouse_id
   AND nwz.zone_type    = 'NORMAL'
SET il.normal_zone_id = nwz.id
WHERE il.normal_zone_id IS NULL;

-- ==============================
-- inbound_lines.damaged_zone_id
-- ==============================
UPDATE inbound_lines il
JOIN inbounds i
    ON il.inbound_id = i.id
JOIN warehouse_zones nwz
    ON nwz.warehouse_id = i.warehouse_id
   AND nwz.zone_type    = 'NORMAL'
LEFT JOIN warehouse_zones dwz
    ON dwz.warehouse_id = i.warehouse_id
   AND dwz.zone_type    = 'DAMAGED'
SET il.damaged_zone_id = COALESCE(dwz.id, nwz.id)
WHERE il.damaged_zone_id IS NULL;

-- ==============================
-- outbound_lines.zone_id
-- ==============================
UPDATE outbound_lines ol
JOIN outbounds o
    ON ol.outbound_id = o.id
JOIN warehouse_zones nwz
    ON nwz.warehouse_id = o.warehouse_id
   AND nwz.zone_type    = 'NORMAL'
SET ol.zone_id = nwz.id
WHERE ol.zone_id IS NULL;

-- ==============================
-- 검증 쿼리 (실행 후 NULL이 남아 있는지 확인)
-- ==============================
-- SELECT COUNT(*) FROM inbound_lines  WHERE normal_zone_id  IS NULL;
-- SELECT COUNT(*) FROM inbound_lines  WHERE damaged_zone_id IS NULL;
-- SELECT COUNT(*) FROM outbound_lines WHERE zone_id         IS NULL;
