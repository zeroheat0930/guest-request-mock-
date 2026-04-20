-- V4: 주차 차량 등록
-- (Flyway 미적용 상태의 placeholder — 상용 전환 시 경로만 유지하고 Flyway 로 wire up)

CREATE TABLE IF NOT EXISTS gr_parking_registration (
	req_no        VARCHAR(30)  NOT NULL,
	prop_cd       VARCHAR(10)  NOT NULL,
	rsv_no        VARCHAR(20)  NOT NULL,
	room_no       VARCHAR(10)  NULL,
	car_no        VARCHAR(20)  NOT NULL,
	car_tp        VARCHAR(20)  NULL,
	req_memo      VARCHAR(200) NULL,
	proc_stat_cd  VARCHAR(10)  NULL,
	req_dt        VARCHAR(8)   NULL,
	req_tm        VARCHAR(4)   NULL,
	PRIMARY KEY (req_no)
);
