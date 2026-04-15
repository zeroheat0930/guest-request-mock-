package com.daol.concierge.ccs.task;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.domain.CcsTask;
import com.daol.concierge.ccs.service.CcsTaskService;
import com.daol.concierge.ccs.util.CcsResponse;
import com.daol.concierge.core.api.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * US-009 — 직원 간 요청 생성 (STAFF_REQ).
 * GET/PUT 은 US-008 의 CcsTaskController 가 담당; 이 파일은 POST 전용.
 */
@RestController
@RequestMapping("/api/ccs/tasks")
public class CcsTaskCreateController {

    @Autowired
    private CcsTaskService taskService;

    /**
     * 직원 간 요청 생성 (STAFF_REQ). 부서/직원 대상 요청을 동료에게 보낸다.
     *
     * Body: { toDeptCd, toAssigneeId (nullable), title, memo (nullable), roomNo (nullable) }
     * Response: PMS 스타일 봉투 { resCd, resMsg, map: { taskId, statusCd } }
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();

        String toDeptCd     = str(body.get("toDeptCd"));
        String toAssigneeId = str(body.get("toAssigneeId"));
        String title        = str(body.get("title"));
        String memo         = str(body.get("memo"));
        String roomNo       = str(body.get("roomNo"));

        if (toDeptCd == null || toDeptCd.isBlank() || title == null || title.isBlank()) {
            throw new BizException("9001", "필수값 누락");
        }

        CcsTask task = taskService.createTask(
                me.propCd(), me.cmpxCd(), "STAFF_REQ", me.staffId(),
                toDeptCd, title, memo, roomNo);

        if (toAssigneeId != null && !toAssigneeId.isBlank()) {
            task = taskService.assignTo(task.getTaskId(), toAssigneeId);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taskId",   task.getTaskId());
        map.put("statusCd", task.getStatusCd());

        return CcsResponse.ok(map);
    }

    private static String str(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).strip();
        return s.isEmpty() ? null : s;
    }
}
