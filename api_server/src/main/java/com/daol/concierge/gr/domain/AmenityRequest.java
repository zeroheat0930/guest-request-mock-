package com.daol.concierge.gr.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 어메니티 요청 (투숙객 → 프론트)
 *
 * 요청 1건에 여러 품목이 포함될 수 있어 @ElementCollection 로 child 테이블 자동 생성.
 */
@Entity
@Table(name = "gr_amenity_request")
public class AmenityRequest {

	@Id
	@Column(name = "req_no", length = 30)
	private String reqNo;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "rsv_no", length = 20, nullable = false)
	private String rsvNo;

	@Column(name = "room_no", length = 10)
	private String roomNo;

	@Column(name = "req_memo", length = 1000)
	private String reqMemo;

	@Column(name = "proc_stat_cd", length = 10)
	private String procStatCd;

	@Column(name = "req_dt", length = 8)
	private String reqDt;

	@Column(name = "req_tm", length = 4)
	private String reqTm;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "gr_amenity_request_item",
			joinColumns = @JoinColumn(name = "req_no")
	)
	private List<AmenityRequestItem> items = new ArrayList<>();

	public AmenityRequest() {}

	public String getReqNo() { return reqNo; }
	public void setReqNo(String v) { this.reqNo = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getRsvNo() { return rsvNo; }
	public void setRsvNo(String v) { this.rsvNo = v; }

	public String getRoomNo() { return roomNo; }
	public void setRoomNo(String v) { this.roomNo = v; }

	public String getReqMemo() { return reqMemo; }
	public void setReqMemo(String v) { this.reqMemo = v; }

	public String getProcStatCd() { return procStatCd; }
	public void setProcStatCd(String v) { this.procStatCd = v; }

	public String getReqDt() { return reqDt; }
	public void setReqDt(String v) { this.reqDt = v; }

	public String getReqTm() { return reqTm; }
	public void setReqTm(String v) { this.reqTm = v; }

	public List<AmenityRequestItem> getItems() { return items; }
	public void setItems(List<AmenityRequestItem> v) { this.items = v; }
}
