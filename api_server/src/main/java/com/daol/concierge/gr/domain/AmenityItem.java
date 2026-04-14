package com.daol.concierge.gr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 어메니티 품목 마스터 (AM001~AM005)
 */
@Entity
@Table(name = "gr_amenity_item")
public class AmenityItem {

	@Id
	@Column(name = "item_cd", length = 10)
	private String itemCd;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "item_nm", length = 60)
	private String itemNm;

	@Column(name = "item_nm_eng", length = 60)
	private String itemNmEng;

	@Column(name = "max_qty")
	private Integer maxQty;

	public AmenityItem() {}

	public String getItemCd() { return itemCd; }
	public void setItemCd(String v) { this.itemCd = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getItemNm() { return itemNm; }
	public void setItemNm(String v) { this.itemNm = v; }

	public String getItemNmEng() { return itemNmEng; }
	public void setItemNmEng(String v) { this.itemNmEng = v; }

	public Integer getMaxQty() { return maxQty; }
	public void setMaxQty(Integer v) { this.maxQty = v; }
}
