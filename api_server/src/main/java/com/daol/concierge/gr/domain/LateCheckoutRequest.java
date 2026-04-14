package com.daol.concierge.gr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 레이트 체크아웃 요청
 */
@Entity
@Table(name = "gr_late_checkout_request")
public class LateCheckoutRequest {

	@Id
	@Column(name = "req_no", length = 30)
	private String reqNo;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "rsv_no", length = 20, nullable = false)
	private String rsvNo;

	@Column(name = "room_no", length = 10)
	private String roomNo;

	@Column(name = "cur_chk_out_tm", length = 4)
	private String curChkOutTm;

	@Column(name = "req_chk_out_tm", length = 4)
	private String reqChkOutTm;

	@Column(name = "add_amt")
	private Integer addAmt;

	@Column(name = "proc_stat_cd", length = 10)
	private String procStatCd;

	@Column(name = "req_dt", length = 8)
	private String reqDt;

	@Column(name = "req_tm", length = 4)
	private String reqTm;

	public LateCheckoutRequest() {}

	public String getReqNo() { return reqNo; }
	public void setReqNo(String v) { this.reqNo = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getRsvNo() { return rsvNo; }
	public void setRsvNo(String v) { this.rsvNo = v; }

	public String getRoomNo() { return roomNo; }
	public void setRoomNo(String v) { this.roomNo = v; }

	public String getCurChkOutTm() { return curChkOutTm; }
	public void setCurChkOutTm(String v) { this.curChkOutTm = v; }

	public String getReqChkOutTm() { return reqChkOutTm; }
	public void setReqChkOutTm(String v) { this.reqChkOutTm = v; }

	public Integer getAddAmt() { return addAmt; }
	public void setAddAmt(Integer v) { this.addAmt = v; }

	public String getProcStatCd() { return procStatCd; }
	public void setProcStatCd(String v) { this.procStatCd = v; }

	public String getReqDt() { return reqDt; }
	public void setReqDt(String v) { this.reqDt = v; }

	public String getReqTm() { return reqTm; }
	public void setReqTm(String v) { this.reqTm = v; }
}
