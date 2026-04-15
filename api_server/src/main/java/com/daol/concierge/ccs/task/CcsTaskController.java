package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.domain.CcsTask;
import com.daol.concierge.ccs.repo.CcsTaskRepository;
import com.daol.concierge.ccs.service.CcsTaskService;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CCS 작업 REST 엔드포인트
 *
 * CcsJwtFilter 가 /api/ccs/** 를 보호하므로, 여기서는 SecurityContext 에서 staff principal 을
 * 꺼내 해당 스태프의 propCd/cmpxCd/deptCd 로 스코프된 작업만 조회한다.
 * 응답 봉투는 PMS 스타일 {resCd, resMsg, map}.
 */
@RestController
@RequestMapping("/api/ccs/tasks")
public class CcsTaskController {

	@Autowired
	private CcsTaskService taskService;

	@Autowired
	private CcsTaskRepository taskRepo;

	private CcsTask requireOwnedTask(String taskId, CcsPrincipal me) {
		CcsTask existing = taskRepo.findById(taskId)
				.orElseThrow(() -> new BizException("9404", "태스크 없음"));
		if (!Objects.equals(me.propCd(), existing.getPropCd())
				|| !Objects.equals(me.cmpxCd(), existing.getCmpxCd())
				|| !Objects.equals(me.deptCd(), existing.getDeptCd())) {
			throw new BizException("9102", "권한 없음");
		}
		return existing;
	}

	@GetMapping
	public Map<String, Object> list(@RequestParam(value = "statusCd", required = false) String statusCd) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		List<CcsTask> tasks = taskService.listForDept(me.propCd(), me.cmpxCd(), me.deptCd(), statusCd);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("list", tasks);
		return envelope(map);
	}

	@PutMapping("/{taskId}/assign")
	public Map<String, Object> assign(@PathVariable String taskId, @RequestBody Map<String, Object> body) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		requireOwnedTask(taskId, me);
		String assigneeId = str(body.get("assigneeId"));
		if (assigneeId == null || assigneeId.isBlank()) {
			throw new BizException("9400", "assigneeId 필수");
		}
		CcsTask t = taskService.assignTo(taskId, assigneeId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("task", t);
		return envelope(map);
	}

	@PutMapping("/{taskId}/status")
	public Map<String, Object> status(@PathVariable String taskId, @RequestBody Map<String, Object> body) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		requireOwnedTask(taskId, me);
		String statusCd = str(body.get("statusCd"));
		if (statusCd == null || statusCd.isBlank()) {
			throw new BizException("9400", "statusCd 필수");
		}
		CcsTask t = taskService.transitionStatus(taskId, statusCd);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("task", t);
		return envelope(map);
	}

	private static Map<String, Object> envelope(Map<String, Object> map) {
		return CcsResponse.ok(map);
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}
}
