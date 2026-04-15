package com.daol.concierge.ccs.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * CcsDepartment 복합키 (propCd + cmpxCd + deptCd)
 */
public class CcsDepartmentId implements Serializable {

	private String propCd;
	private String cmpxCd;
	private String deptCd;

	public CcsDepartmentId() {}

	public CcsDepartmentId(String propCd, String cmpxCd, String deptCd) {
		this.propCd = propCd;
		this.cmpxCd = cmpxCd;
		this.deptCd = deptCd;
	}

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getCmpxCd() { return cmpxCd; }
	public void setCmpxCd(String v) { this.cmpxCd = v; }

	public String getDeptCd() { return deptCd; }
	public void setDeptCd(String v) { this.deptCd = v; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CcsDepartmentId that)) return false;
		return Objects.equals(propCd, that.propCd)
				&& Objects.equals(cmpxCd, that.cmpxCd)
				&& Objects.equals(deptCd, that.deptCd);
	}

	@Override
	public int hashCode() {
		return Objects.hash(propCd, cmpxCd, deptCd);
	}
}
