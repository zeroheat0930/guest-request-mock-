package com.daol.concierge.ccs.service;

import com.daol.concierge.ccs.domain.CcsTask;
import com.daol.concierge.ccs.repo.CcsTaskRepository;
import com.daol.concierge.core.api.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CCS 작업 서비스 — 부서 작업 큐 + 상태 머신
 *
 * 상태 전이:
 *   REQ      → ASSIGNED, CANCELED
 *   ASSIGNED → IN_PROG,  CANCELED
 *   IN_PROG  → DONE,     CANCELED
 *   DONE / CANCELED 는 종착.
 *
 * 비정상 전이 시 BizException("9005","상태 전이 불가").
 */
@Service
public class CcsTaskService {

	@Autowired private CcsTaskRepository ccsTaskRepo;
	@Autowired(required = false) SimpMessagingTemplate messagingTemplate;

	@Transactional
	public CcsTask createTask(String propCd, String cmpxCd, String sourceType, String sourceRefNo,
			String deptCd, String title, String memo, String roomNo) {
		LocalDateTime now = LocalDateTime.now();
		String taskId = "CT" + System.currentTimeMillis()
				+ String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

		CcsTask t = new CcsTask();
		t.setTaskId(taskId);
		t.setPropCd(propCd);
		t.setCmpxCd(cmpxCd);
		t.setSourceType(sourceType);
		t.setSourceRefNo(sourceRefNo);
		t.setDeptCd(deptCd);
		t.setTitle(title);
		t.setMemo(memo);
		t.setRoomNo(roomNo);
		t.setStatusCd("REQ");
		t.setCreatedAt(now);
		t.setUpdatedAt(now);
		CcsTask saved = ccsTaskRepo.save(t);
		publish(saved);
		return saved;
	}

	@Transactional
	public CcsTask assignTo(String taskId, String assigneeId) {
		CcsTask t = ccsTaskRepo.findById(taskId)
				.orElseThrow(() -> new BizException("9404", "작업 없음"));
		if (!"REQ".equals(t.getStatusCd())) {
			throw new BizException("9005", "상태 전이 불가");
		}
		t.setAssigneeId(assigneeId);
		t.setStatusCd("ASSIGNED");
		t.setUpdatedAt(LocalDateTime.now());
		CcsTask saved = ccsTaskRepo.save(t);
		publish(saved);
		return saved;
	}

	@Transactional
	public CcsTask transitionStatus(String taskId, String newStatusCd) {
		CcsTask t = ccsTaskRepo.findById(taskId)
				.orElseThrow(() -> new BizException("9404", "작업 없음"));
		String cur = t.getStatusCd();

		boolean legal = false;
		if ("CANCELED".equals(newStatusCd)) {
			legal = "REQ".equals(cur) || "ASSIGNED".equals(cur) || "IN_PROG".equals(cur);
		} else if ("ASSIGNED".equals(newStatusCd)) {
			legal = "REQ".equals(cur);
		} else if ("IN_PROG".equals(newStatusCd)) {
			legal = "ASSIGNED".equals(cur);
		} else if ("DONE".equals(newStatusCd)) {
			legal = "IN_PROG".equals(cur);
		}

		if (!legal) {
			throw new BizException("9005", "상태 전이 불가");
		}

		t.setStatusCd(newStatusCd);
		t.setUpdatedAt(LocalDateTime.now());
		CcsTask saved = ccsTaskRepo.save(t);
		publish(saved);
		return saved;
	}

	@Transactional(readOnly = true)
	public List<CcsTask> listForDept(String propCd, String cmpxCd, String deptCd, String statusCd) {
		if (statusCd != null && !statusCd.isEmpty()) {
			return ccsTaskRepo.findByPropCdAndCmpxCdAndDeptCdAndStatusCdOrderByCreatedAtDesc(
					propCd, cmpxCd, deptCd, statusCd);
		}
		return ccsTaskRepo.findByPropCdAndCmpxCdAndDeptCdOrderByCreatedAtDesc(propCd, cmpxCd, deptCd);
	}

	private void publish(CcsTask task) {
		if (messagingTemplate == null) return;
		// Defer publish until after transaction commits to avoid subscribers reading uncommitted state
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			doPublish(task);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				doPublish(task);
			}
		});
	}

	private void doPublish(CcsTask task) {
		messagingTemplate.convertAndSend("/topic/ccs/dept/" + task.getDeptCd(), task);
		if (task.getAssigneeId() != null && !task.getAssigneeId().isEmpty()) {
			messagingTemplate.convertAndSend("/topic/ccs/staff/" + task.getAssigneeId(), task);
		}
	}
}
