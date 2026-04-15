package com.daol.concierge.ccs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 컨시어지 운영 스태프 (프론트/하우스키핑/매니저 등)
 *
 * loginId 는 (propCd, cmpxCd) 단위로 unique. PMS USER_TP 컬럼 컨벤션을 부분 차용.
 */
@Entity
@Table(
		name = "ccs_staff",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_ccs_staff_login",
				columnNames = {"prop_cd", "cmpx_cd", "login_id"}
		)
)
public class CcsStaff {

	@Id
	@Column(name = "staff_id", length = 20, nullable = false)
	private String staffId;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "cmpx_cd", length = 5, nullable = false)
	private String cmpxCd;

	@Column(name = "dept_cd", length = 10, nullable = false)
	private String deptCd;

	@Column(name = "login_id", length = 50, nullable = false)
	private String loginId;

	@Column(name = "password_hash", length = 100, nullable = false)
	private String passwordHash;

	@Column(name = "staff_nm", length = 50, nullable = false)
	private String staffNm;

	@Column(name = "position_cd", length = 10)
	private String positionCd;

	@Column(name = "use_yn", length = 1, nullable = false)
	private String useYn = "Y";

	@Column(name = "reg_dt")
	private LocalDateTime regDt;

	public CcsStaff() {}

	@PrePersist
	public void prePersist() {
		if (this.staffId == null || this.staffId.isEmpty()) {
			this.staffId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
		}
		if (this.regDt == null) {
			this.regDt = LocalDateTime.now();
		}
		if (this.useYn == null || this.useYn.isEmpty()) {
			this.useYn = "Y";
		}
	}

	public String getStaffId() { return staffId; }
	public void setStaffId(String v) { this.staffId = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getCmpxCd() { return cmpxCd; }
	public void setCmpxCd(String v) { this.cmpxCd = v; }

	public String getDeptCd() { return deptCd; }
	public void setDeptCd(String v) { this.deptCd = v; }

	public String getLoginId() { return loginId; }
	public void setLoginId(String v) { this.loginId = v; }

	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String v) { this.passwordHash = v; }

	public String getStaffNm() { return staffNm; }
	public void setStaffNm(String v) { this.staffNm = v; }

	public String getPositionCd() { return positionCd; }
	public void setPositionCd(String v) { this.positionCd = v; }

	public String getUseYn() { return useYn; }
	public void setUseYn(String v) { this.useYn = v; }

	public LocalDateTime getRegDt() { return regDt; }
	public void setRegDt(LocalDateTime v) { this.regDt = v; }
}
