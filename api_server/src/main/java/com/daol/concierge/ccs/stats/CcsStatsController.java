package com.daol.concierge.ccs.stats;

import com.daol.concierge.ccs.auth.CcsPrincipal;
import com.daol.concierge.ccs.auth.CcsSecurityContextUtil;
import com.daol.concierge.ccs.domain.CcsTask;
import com.daol.concierge.ccs.repo.CcsTaskRepository;
import com.daol.concierge.ccs.util.CcsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * US-012: 오늘 부서 통계 (접수 건수 / 완료 건수 / 평균 처리 시간)
 *
 * GET /api/ccs/stats/today?deptCd=optional
 * - deptCd 미전달 시 로그인 스태프의 deptCd 사용
 */
@RestController
@RequestMapping("/api/ccs/stats")
public class CcsStatsController {

    @Autowired
    private CcsTaskRepository taskRepo;

    @GetMapping("/today")
    public Map<String, Object> today(@RequestParam(required = false) String deptCd) {
        CcsPrincipal me = CcsSecurityContextUtil.requireCcsPrincipal();
        String dept = (deptCd != null && !deptCd.isBlank()) ? deptCd : me.deptCd();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long received = taskRepo.countByDeptCdAndCreatedAtBetween(dept, startOfDay, endOfDay);
        long completed = taskRepo.countByDeptCdAndStatusCdAndUpdatedAtBetween(dept, "DONE", startOfDay, endOfDay);

        List<CcsTask> doneToday = taskRepo.findByDeptCdAndStatusCdAndUpdatedAtBetween(dept, "DONE", startOfDay, endOfDay);
        Double avgMinutes = null;
        if (!doneToday.isEmpty()) {
            long totalSec = 0;
            for (CcsTask t : doneToday) {
                totalSec += java.time.Duration.between(t.getCreatedAt(), t.getUpdatedAt()).getSeconds();
            }
            avgMinutes = (totalSec / (double) doneToday.size()) / 60.0;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("received", received);
        map.put("completed", completed);
        map.put("avgMinutes", avgMinutes);

        return CcsResponse.ok(map);
    }
}
