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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/ccs/tasks")
public class CcsTaskCreateController extends BaseController {

	@Autowired private CcsTaskService taskService;

	@ResponseBody
	@RequestMapping(method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse create(RequestParams requestParams) {
		CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();

		String toDeptCd = requestParams.getString("toDeptCd");
		String toAssigneeId = requestParams.getString("toAssigneeId");
		String title = requestParams.getString("title");
		String memo = requestParams.getString("memo");
		String roomNo = requestParams.getString("roomNo");

		if (toDeptCd == null || toDeptCd.isBlank() || title == null || title.isBlank())
			throw new ApiException(ApiStatus.SYSTEM_ERROR, "필수값 누락");

		Map<String, Object> task = taskService.createTask(
				me.propCd(), me.cmpxCd(), "STAFF_REQ", me.staffId(),
				toDeptCd, title, memo, roomNo);

		if (toAssigneeId != null && !toAssigneeId.isBlank()) {
			String taskId = str(task.get("taskId"));
			task = taskService.assignTo(taskId, toAssigneeId);
		}

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("taskId", str(task.get("taskId")));
		map.put("statusCd", str(task.get("statusCd")));
		return Responses.MapResponse.of(map);
	}

	private static String str(Object o) {
		if (o == null) return null;
		String s = String.valueOf(o).strip();
		return s.isEmpty() ? null : s;
	}
}
