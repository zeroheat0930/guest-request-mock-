package com.daol.concierge.gr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 주차 차량 등록 (투숙객 → 프론트)
 */
@Entity
@Table(name = "gr_parking_registration")
public class ParkingRegistration {

	@Id
	@Column(name = "req_no", length = 30)
	private String reqNo;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "rsv_no", length = 20, nullable = false)
	private String rsvNo;

	@Column(name = "room_no", length = 10)
	private String roomNo;

	@Column(name = "car_no", length = 20, nullable = false)
	private String carNo;

	@Column(name = "car_tp", length = 20)
	private String carTp;

	@Column(name = "req_memo", length = 200)
	private String reqMemo;

	@Column(name = "proc_stat_cd", length = 10)
	private String procStatCd;

	@Column(name = "req_dt", length = 8)
	private String reqDt;

	@Column(name = "req_tm", length = 4)
	private String reqTm;

	public ParkingRegistration() {}

	public String getReqNo() { return reqNo; }
	public void setReqNo(String v) { this.reqNo = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getRsvNo() { return rsvNo; }
	public void setRsvNo(String v) { this.rsvNo = v; }

	public String getRoomNo() { return roomNo; }
	public void setRoomNo(String v) { this.roomNo = v; }

	public String getCarNo() { return carNo; }
	public void setCarNo(String v) { this.carNo = v; }

	public String getCarTp() { return carTp; }
	public void setCarTp(String v) { this.carTp = v; }

	public String getReqMemo() { return reqMemo; }
	public void setReqMemo(String v) { this.reqMemo = v; }

	public String getProcStatCd() { return procStatCd; }
	public void setProcStatCd(String v) { this.procStatCd = v; }

	public String getReqDt() { return reqDt; }
	public void setReqDt(String v) { this.reqDt = v; }

	public String getReqTm() { return reqTm; }
	public void setReqTm(String v) { this.reqTm = v; }
}
