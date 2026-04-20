-- V3: concierge_property 에 좌표 컬럼 추가 (NEARBY 기능용)
-- 개발(H2)은 ddl-auto=update 로 자동 적용되지만, 프로덕션(MariaDB, ddl-auto=validate)은
-- 사내 DDL 변경 절차를 거쳐 이 스크립트를 수동 적용한다.
ALTER TABLE concierge_property ADD COLUMN lat DOUBLE NULL;
ALTER TABLE concierge_property ADD COLUMN lng DOUBLE NULL;

UPDATE concierge_property SET lat = 37.5665, lng = 126.9780 WHERE prop_cd = 'HQ';
