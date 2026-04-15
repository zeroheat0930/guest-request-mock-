package com.daol.concierge.feature;

import java.io.Serializable;
import java.util.Objects;

/**
 * ConciergeFeature 복합키 (propCd + featureCd)
 */
public class ConciergeFeatureId implements Serializable {

	private String propCd;
	private String featureCd;

	public ConciergeFeatureId() {}

	public ConciergeFeatureId(String propCd, String featureCd) {
		this.propCd = propCd;
		this.featureCd = featureCd;
	}

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getFeatureCd() { return featureCd; }
	public void setFeatureCd(String v) { this.featureCd = v; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ConciergeFeatureId that)) return false;
		return Objects.equals(propCd, that.propCd) && Objects.equals(featureCd, that.featureCd);
	}

	@Override
	public int hashCode() {
		return Objects.hash(propCd, featureCd);
	}
}
