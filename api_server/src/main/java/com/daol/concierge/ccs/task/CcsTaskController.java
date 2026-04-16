package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.service.CcsTaskService;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/ccs/tasks")
public class CcsTaskController {

	@Autowired private CcsTaskService taskService;
	@Autowired private InvMapper invMapper;

	private void requireOwned(String taskId, CcsPrincipal me) {
		Map<String, Object> t = invMapper.selectTask(taskId);
		if (t == null) throw new BizException("9404", "태스크 없음");
		if (!Objects.equals(me.propCd(), str(t.get("propCd")))
				|| !Objects.equals(me.cmpxCd(), str(t.get("cmpxCd")))
				|| !Objects.equals(me.deptCd(), str(t.get("deptCd"))))
			throw new BizException("9102", "권한 없음");
	}

	@GetMapping
	public Map<String, Object> list(@RequestParam(value = "statusCd", required = false) String statusCd) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		List<Map<String, Object>> tasks = taskService.listForDept(me.propCd(), me.cmpxCd(), me.deptCd(), statusCd);
		return CcsResponse.ok(Map.of("list", tasks));
	}

	@PutMapping("/{taskId}/assign")
	public Map<String, Object> assign(@PathVariable String taskId, @RequestBody Map<String, Object> body) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		requireOwned(taskId, me);
		String assigneeId = str(body.get("assigneeId"));
		if (assigneeId == null || assigneeId.isBlank()) throw new BizException("9400", "assigneeId 필수");
		Map<String, Object> t = taskService.assignTo(taskId, assigneeId);
		return CcsResponse.ok(Map.of("task", t));
	}

	@PutMapping("/{taskId}/status")
	public Map<String, Object> status(@PathVariable String taskId, @RequestBody Map<String, Object> body) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		requireOwned(taskId, me);
		String statusCd = str(body.get("statusCd"));
		if (statusCd == null || statusCd.isBlank()) throw new BizException("9400", "statusCd 필수");
		Map<String, Object> t = taskService.transitionStatus(taskId, statusCd);
		return CcsResponse.ok(Map.of("task", t));
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
