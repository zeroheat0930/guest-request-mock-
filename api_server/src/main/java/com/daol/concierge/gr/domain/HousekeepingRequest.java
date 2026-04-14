package com.daol.concierge.gr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 하우스키핑 상태 변경 요청 (MU=객실정비, DND=방해금지, CLR=해제)
 */
@Entity
@Table(name = "gr_housekeeping_request")
public class HousekeepingRequest {

	@Id
	@Column(name = "req_no", length = 30)
	private String reqNo;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "rsv_no", length = 20, nullable = false)
	private String rsvNo;

	@Column(name = "room_no", length = 10)
	private String roomNo;

	@Column(name = "hk_stat_cd", length = 10, nullable = false)
	private String hkStatCd;

	@Column(name = "req_memo", length = 1000)
	private String reqMemo;

	@Column(name = "req_dt", length = 8)
	private String reqDt;

	@Column(name = "req_tm", length = 4)
	private String reqTm;

	public HousekeepingRequest() {}

	public String getReqNo() { return reqNo; }
	public void setReqNo(String v) { this.reqNo = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getRsvNo() { return rsvNo; }
	public void setRsvNo(String v) { this.rsvNo = v; }

	public String getRoomNo() { return roomNo; }
	public void setRoomNo(String v) { this.roomNo = v; }

	public String getHkStatCd() { return hkStatCd; }
	public void setHkStatCd(String v) { this.hkStatCd = v; }

	public String getReqMemo() { return reqMemo; }
	public void setReqMemo(String v) { this.reqMemo = v; }

	public String getReqDt() { return reqDt; }
	public void setReqDt(String v) { this.reqDt = v; }

	public String getReqTm() { return reqTm; }
	public void setReqTm(String v) { this.reqTm = v; }
}
