package com.daol.concierge.ccs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * 컨시어지 운영 부서 마스터 (PROP_CD + CMPX_CD + DEPT_CD 스코프)
 */
@Entity
@Table(name = "ccs_department")
@IdClass(CcsDepartmentId.class)
public class CcsDepartment {

	@Id
	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Id
	@Column(name = "cmpx_cd", length = 5, nullable = false)
	private String cmpxCd;

	@Id
	@Column(name = "dept_cd", length = 10, nullable = false)
	private String deptCd;

	@Column(name = "dept_nm", length = 50, nullable = false)
	private String deptNm;

	@Column(name = "sort_ord", nullable = false)
	private Integer sortOrd = 0;

	@Column(name = "use_yn", length = 1, nullable = false)
	private String useYn = "Y";

	public CcsDepartment() {}

	public CcsDepartment(String propCd, String cmpxCd, String deptCd, String deptNm) {
		this.propCd = propCd;
		this.cmpxCd = cmpxCd;
		this.deptCd = deptCd;
		this.deptNm = deptNm;
	}

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getCmpxCd() { return cmpxCd; }
	public void setCmpxCd(String v) { this.cmpxCd = v; }

	public String getDeptCd() { return deptCd; }
	public void setDeptCd(String v) { this.deptCd = v; }

	public String getDeptNm() { return deptNm; }
	public void setDeptNm(String v) { this.deptNm = v; }

	public Integer getSortOrd() { return sortOrd; }
	public void setSortOrd(Integer v) { this.sortOrd = v; }

	public String getUseYn() { return useYn; }
	public void setUseYn(String v) { this.useYn = v; }
}
