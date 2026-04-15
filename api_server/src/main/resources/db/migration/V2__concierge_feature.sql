-- V2: concierge feature flag + property master
-- (Flyway 미적용 상태의 placeholder — 상용 전환 시 경로만 유지하고 Flyway 로 wire up)

CREATE TABLE IF NOT EXISTS concierge_property (
	prop_cd       VARCHAR(10)  NOT NULL,
	prop_nm       VARCHAR(100) NOT NULL,
	prop_nm_eng   VARCHAR(100) NULL,
	timezone      VARCHAR(40)  NULL,
	default_lang  VARCHAR(10)  NULL,
	PRIMARY KEY (prop_cd)
);

CREATE TABLE IF NOT EXISTS concierge_feature (
	prop_cd      VARCHAR(10) NOT NULL,
	feature_cd   VARCHAR(20) NOT NULL,
	use_yn       CHAR(1)     NOT NULL,
	sort_ord     INT         NOT NULL,
	config_json  TEXT        NULL,
	upd_user     VARCHAR(20) NULL,
	upd_dt       DATETIME    NULL,
	PRIMARY KEY (prop_cd, feature_cd)
);
