package com.daol.concierge.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 컨시어지가 서비스하는 프로퍼티(호텔) 마스터
 */
@Entity
@Table(name = "concierge_property")
public class ConciergeProperty {

	@Id
	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "prop_nm", length = 100, nullable = false)
	private String propNm;

	@Column(name = "prop_nm_eng", length = 100)
	private String propNmEng;

	@Column(name = "timezone", length = 40)
	private String timezone;

	@Column(name = "default_lang", length = 10)
	private String defaultLang;

	@Column(name = "lat")
	private Double lat;

	@Column(name = "lng")
	private Double lng;

	public ConciergeProperty() {}

	public ConciergeProperty(String propCd, String propNm) {
		this.propCd = propCd;
		this.propNm = propNm;
	}

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getPropNm() { return propNm; }
	public void setPropNm(String v) { this.propNm = v; }

	public String getPropNmEng() { return propNmEng; }
	public void setPropNmEng(String v) { this.propNmEng = v; }

	public String getTimezone() { return timezone; }
	public void setTimezone(String v) { this.timezone = v; }

	public String getDefaultLang() { return defaultLang; }
	public void setDefaultLang(String v) { this.defaultLang = v; }

	public Double getLat() { return lat; }
	public void setLat(Double v) { this.lat = v; }

	public Double getLng() { return lng; }
	public void setLng(Double v) { this.lng = v; }
}
