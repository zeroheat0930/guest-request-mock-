package com.daol.concierge.gr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 어메니티 요청 상세 (요청 1건에 여러 품목)
 */
@Embeddable
public class AmenityRequestItem {

	@Column(name = "item_cd", length = 10, nullable = false)
	private String itemCd;

	@Column(name = "qty", nullable = false)
	private Integer qty;

	public AmenityRequestItem() {}

	public AmenityRequestItem(String itemCd, Integer qty) {
		this.itemCd = itemCd;
		this.qty = qty;
	}

	public String getItemCd() { return itemCd; }
	public void setItemCd(String v) { this.itemCd = v; }

	public Integer getQty() { return qty; }
	public void setQty(Integer v) { this.qty = v; }
}
