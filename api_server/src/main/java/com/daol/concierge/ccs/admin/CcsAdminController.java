package com.daol.concierge.ccs.admin;

import com.daol.concierge.ccs.domain.CcsDepartment;
import com.daol.concierge.ccs.domain.CcsStaff;
import com.daol.concierge.ccs.repo.CcsDepartmentRepository;
import com.daol.concierge.ccs.repo.CcsStaffRepository;
import com.daol.concierge.ccs.util.CcsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * US-013: CCS 관리자 API (부서·직원 조회)
 *
 * /api/concierge/admin/** 은 AdminWebMvcConfig + AdminAuthInterceptor 가
 * X-Admin-Token 으로 이미 보호하므로 별도 인증 로직 불필요.
 *
 * NOTE: 라우팅 룰 편집은 현재 CcsRoutingRuleDefault 에 하드코딩되어 있으므로
 * PUT /routing-rules 는 MVP 범위 밖. 향후 별도 US에서 구현 예정.
 */
@RestController
@RequestMapping("/api/concierge/admin")
public class CcsAdminController {

    @Autowired
    private CcsDepartmentRepository deptRepo;

    @Autowired
    private CcsStaffRepository staffRepo;

    @GetMapping("/departments")
    public Map<String, Object> listDepartments(
            @RequestParam String propCd,
            @RequestParam String cmpxCd) {
        List<CcsDepartment> depts =
                deptRepo.findByPropCdAndCmpxCdAndUseYnOrderBySortOrdAsc(propCd, cmpxCd, "Y");
        return wrap(Map.of("list", depts));
    }

    @GetMapping("/staff")
    public Map<String, Object> listStaff(
            @RequestParam String propCd,
            @RequestParam String cmpxCd,
            @RequestParam(required = false) String deptCd) {
        List<CcsStaff> staff;
        if (deptCd != null && !deptCd.isBlank()) {
            staff = staffRepo.findByPropCdAndCmpxCdAndDeptCdOrderByStaffNm(propCd, cmpxCd, deptCd);
        } else {
            staff = staffRepo.findByPropCdAndCmpxCdOrderByStaffNm(propCd, cmpxCd);
        }
        // password_hash 제외하고 응답
        List<Map<String, Object>> out = new ArrayList<>();
        for (CcsStaff s : staff) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("staffId", s.getStaffId());
            row.put("loginId", s.getLoginId());
            row.put("staffNm", s.getStaffNm());
            row.put("deptCd", s.getDeptCd());
            row.put("positionCd", s.getPositionCd());
            row.put("useYn", s.getUseYn());
            out.add(row);
        }
        return wrap(Map.of("list", out));
    }

    private static Map<String, Object> wrap(Map<String, Object> map) {
        return CcsResponse.ok(map);
    }
}
