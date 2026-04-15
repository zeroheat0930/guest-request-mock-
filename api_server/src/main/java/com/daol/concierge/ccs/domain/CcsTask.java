package com.daol.concierge.ccs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 컨시어지 컨트롤 시스템(CCS) 작업 엔터티
 *
 * 게스트/스태프 요청을 부서별 작업 큐로 받아서 상태 머신으로 처리.
 * sourceType / statusCd 는 PMS 스타일 그대로 String 코드로 유지 (Java enum 안 씀).
 */
@Entity
@Table(name = "ccs_task")
public class CcsTask {

	@Id
	@Column(name = "task_id", length = 30)
	private String taskId;

	@Column(name = "prop_cd", length = 10, nullable = false)
	private String propCd;

	@Column(name = "cmpx_cd", length = 5)
	private String cmpxCd;

	@Column(name = "source_type", length = 20, nullable = false)
	private String sourceType;

	@Column(name = "source_ref_no", length = 30)
	private String sourceRefNo;

	@Column(name = "dept_cd", length = 10, nullable = false)
	private String deptCd;

	@Column(name = "assignee_id", length = 20)
	private String assigneeId;

	@Column(name = "status_cd", length = 10, nullable = false)
	private String statusCd;

	@Column(name = "title", length = 100)
	private String title;

	@Column(name = "memo", length = 500)
	private String memo;

	@Column(name = "room_no", length = 10)
	private String roomNo;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public CcsTask() {}

	public String getTaskId() { return taskId; }
	public void setTaskId(String v) { this.taskId = v; }

	public String getPropCd() { return propCd; }
	public void setPropCd(String v) { this.propCd = v; }

	public String getCmpxCd() { return cmpxCd; }
	public void setCmpxCd(String v) { this.cmpxCd = v; }

	public String getSourceType() { return sourceType; }
	public void setSourceType(String v) { this.sourceType = v; }

	public String getSourceRefNo() { return sourceRefNo; }
	public void setSourceRefNo(String v) { this.sourceRefNo = v; }

	public String getDeptCd() { return deptCd; }
	public void setDeptCd(String v) { this.deptCd = v; }

	public String getAssigneeId() { return assigneeId; }
	public void setAssigneeId(String v) { this.assigneeId = v; }

	public String getStatusCd() { return statusCd; }
	public void setStatusCd(String v) { this.statusCd = v; }

	public String getTitle() { return title; }
	public void setTitle(String v) { this.title = v; }

	public String getMemo() { return memo; }
	public void setMemo(String v) { this.memo = v; }

	public String getRoomNo() { return roomNo; }
	public void setRoomNo(String v) { this.roomNo = v; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
