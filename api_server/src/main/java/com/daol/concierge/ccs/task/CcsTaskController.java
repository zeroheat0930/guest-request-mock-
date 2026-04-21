package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.service.CcsTaskService;
import com.daol.concierge.core.api.ApiException;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.ApiStatus;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.inv.mapper.InvMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "CCS Task", description = "스태프 작업 관리 (조회/배정/상태 전환)")
@Controller
@RequestMapping("/api/ccs/tasks")
public class CcsTaskController extends BaseController {

	@Autowired private CcsTaskService taskService;
	@Autowired private InvMapper invMapper;

	private void requireOwned(String taskId, CcsPrincipal me) {
		Map<String, Object> t = invMapper.selectTask(taskId);
		if (t == null) throw new ApiException(ApiStatus.NOT_FOUND, "태스크 없음");
		boolean isAdmin = "00000".equals(me.deptCd());
		if (!isAdmin) {
			if (!Objects.equals(me.propCd(), str(t.get("propCd")))
					|| !Objects.equals(me.cmpxCd(), str(t.get("cmpxCd")))
					|| !Objects.equals(me.deptCd(), str(t.get("deptCd"))))
				throw new ApiException(ApiStatus.ACCESS_DENIED, "권한 없음");
		}
	}

	@Operation(summary = "작업 목록 조회", description = "로그인 스태프의 부서 태스크 (관리자는 전체)")
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse list(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		String statusCd = requestParams.getString("statusCd");
		List<Map<String, Object>> tasks = taskService.listForDept(me.propCd(), me.cmpxCd(), me.deptCd(), statusCd);
		return Responses.MapResponse.of(Map.of("list", tasks));
	}

	@Operation(summary = "작업 배정", description = "assigneeId 를 태스크에 연결, 상태 IN_PROG")
	@ResponseBody
	@RequestMapping(value = "/{taskId}/assign", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse assign(@PathVariable String taskId, RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		requireOwned(taskId, me);
		String assigneeId = requestParams.getString("assigneeId");
		if (assigneeId == null || assigneeId.isBlank()) throw new ApiException(ApiStatus.BAD_REQUEST, "assigneeId 필수");
		Map<String, Object> t = taskService.assignTo(taskId, assigneeId);
		return Responses.MapResponse.of(Map.of("task", t));
	}

	@Operation(summary = "상태 전환", description = "REQ → IN_PROG → DONE / CANCELED")
	@ResponseBody
	@RequestMapping(value = "/{taskId}/status", method = RequestMethod.PUT, produces = APPLICATION_JSON)
	public ApiResponse status(@PathVariable String taskId, RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
		requireOwned(taskId, me);
		String statusCd = requestParams.getString("statusCd");
		if (statusCd == null || statusCd.isBlank()) throw new ApiException(ApiStatus.BAD_REQUEST, "statusCd 필수");
		Map<String, Object> t = taskService.transitionStatus(taskId, statusCd);
		return Responses.MapResponse.of(Map.of("task", t));
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
