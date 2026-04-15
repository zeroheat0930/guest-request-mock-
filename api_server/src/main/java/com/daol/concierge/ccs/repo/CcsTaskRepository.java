package com.daol.concierge.ccs.repo;

import com.daol.concierge.ccs.domain.CcsTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CCS 작업 리포지토리
 */
public interface CcsTaskRepository extends JpaRepository<CcsTask, String> {

	List<CcsTask> findByPropCdAndCmpxCdAndDeptCdAndStatusCdOrderByCreatedAtDesc(
			String propCd, String cmpxCd, String deptCd, String statusCd);

	List<CcsTask> findByPropCdAndCmpxCdAndDeptCdOrderByCreatedAtDesc(
			String propCd, String cmpxCd, String deptCd);

	List<CcsTask> findByAssigneeIdOrderByCreatedAtDesc(String assigneeId);

	long countByPropCdAndCmpxCdAndDeptCdAndStatusCd(
			String propCd, String cmpxCd, String deptCd, String statusCd);

	long countByAssigneeIdAndStatusCd(String assigneeId, String statusCd);

	long countByDeptCdAndCreatedAtBetween(String deptCd, LocalDateTime from, LocalDateTime to);
	long countByDeptCdAndStatusCdAndUpdatedAtBetween(String deptCd, String statusCd, LocalDateTime from, LocalDateTime to);
	List<CcsTask> findByDeptCdAndStatusCdAndUpdatedAtBetween(String deptCd, String statusCd, LocalDateTime from, LocalDateTime to);
}
