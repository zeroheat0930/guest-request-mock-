package com.daol.concierge.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 컨시어지 기능 플래그 (프로퍼티별)
 *
 * AMENITY / HK / LATE_CO / CHAT / NEARBY / PARKING 6개 기능의 on/off + 정렬순서 + 옵션 JSON.
 */
@Entity
@Table(name = "concierge_feature")
@IdClass(ConciergeFeatureId.class)
public class ConciergeFeature {

	@Id
	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Id
	@Column(name = "feature_cd", length = 20, nullable = false)
	private String featureCd;

	@Column(name = "use_yn", length = 1, nullable = false)
	private String useYn;

	@Column(name = "sort_ord", nullable = false)
	private Integer sortOrd;

	@Lob
	@Column(name = "config_json", columnDefinition = "TEXT")
	private String configJson;

	@Column(name = "upd_user", length = 20)
	private String updUser;

	@Column(name = "upd_dt")
	private LocalDateTime updDt;

	public ConciergeFeature() {}

	public ConciergeFeature(String propCd, String featureCd, String useYn, Integer sortOrd) {
		this.propCd = propCd;
		this.featureCd = featureCd;
		this.useYn = useYn;
		this.sortOrd = sortOrd;
	}

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getFeatureCd() { return featureCd; }
	public void setFeatureCd(String v) { this.featureCd = v; }

	public String getUseYn() { return useYn; }
	public void setUseYn(String v) { this.useYn = v; }

	public Integer getSortOrd() { return sortOrd; }
	public void setSortOrd(Integer v) { this.sortOrd = v; }

	public String getConfigJson() { return configJson; }
	public void setConfigJson(String v) { this.configJson = v; }

	public String getUpdUser() { return updUser; }
	public void setUpdUser(String v) { this.updUser = v; }

	public LocalDateTime getUpdDt() { return updDt; }
	public void setUpdDt(LocalDateTime v) { this.updDt = v; }
}
