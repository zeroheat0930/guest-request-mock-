package com.daol.concierge.ccs.repo;

import com.daol.concierge.ccs.domain.CcsDepartment;
import com.daol.concierge.ccs.domain.CcsDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CcsDepartmentRepository extends JpaRepository<CcsDepartment, CcsDepartmentId> {

	List<CcsDepartment> findByPropCdAndCmpxCdAndUseYnOrderBySortOrdAsc(String propCd, String cmpxCd, String useYn);
}
