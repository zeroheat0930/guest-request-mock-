package com.daol.concierge.gr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 예약 마스터
 *
 * PMS 스타일 네이밍 유지 — camelCase 약어 사용.
 * propCd 는 멀티 프로퍼티(호텔 체인) 격리를 위한 필수 필드. 단일 프로퍼티 환경에서도 기본값 "HQ" 로 동작.
 */
@Entity
@Table(name = "gr_reservation")
public class Reservation {

	@Id
	@Column(name = "rsv_no", length = 20)
	private String rsvNo;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "room_no", length = 10)
	private String roomNo;

	@Column(name = "per_nm", length = 80)
	private String perNm;

	@Column(name = "chk_in_dt", length = 8)
	private String chkInDt;

	@Column(name = "chk_out_dt", length = 8)
	private String chkOutDt;

	@Column(name = "chk_out_tm", length = 4)
	private String chkOutTm;

	@Column(name = "room_tp_cd", length = 10)
	private String roomTpCd;

	@Column(name = "per_use_lang", length = 10)
	private String perUseLang;

	/** 게스트 토큰 발급 시 본인 확인용 (yyyyMMdd) */
	@Column(name = "birth_dt", length = 8)
	private String birthDt;

	public Reservation() {}

	public String getRsvNo() { return rsvNo; }
	public void setRsvNo(String v) { this.rsvNo = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getRoomNo() { return roomNo; }
	public void setRoomNo(String v) { this.roomNo = v; }

	public String getPerNm() { return perNm; }
	public void setPerNm(String v) { this.perNm = v; }

	public String getChkInDt() { return chkInDt; }
	public void setChkInDt(String v) { this.chkInDt = v; }

	public String getChkOutDt() { return chkOutDt; }
	public void setChkOutDt(String v) { this.chkOutDt = v; }

	public String getChkOutTm() { return chkOutTm; }
	public void setChkOutTm(String v) { this.chkOutTm = v; }

	public String getRoomTpCd() { return roomTpCd; }
	public void setRoomTpCd(String v) { this.roomTpCd = v; }

	public String getPerUseLang() { return perUseLang; }
	public void setPerUseLang(String v) { this.perUseLang = v; }

	public String getBirthDt() { return birthDt; }
	public void setBirthDt(String v) { this.birthDt = v; }
}
