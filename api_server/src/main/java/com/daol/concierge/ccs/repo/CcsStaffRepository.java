package com.daol.concierge.ccs.repo;

import com.daol.concierge.ccs.domain.CcsStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CcsStaffRepository extends JpaRepository<CcsStaff, String> {

	Optional<CcsStaff> findByLoginIdAndPropCdAndCmpxCd(String loginId, String propCd, String cmpxCd);

	List<CcsStaff> findByPropCdAndCmpxCdAndDeptCdOrderByStaffNm(String propCd, String cmpxCd, String deptCd);

	List<CcsStaff> findByPropCdAndCmpxCdOrderByStaffNm(String propCd, String cmpxCd);

	boolean existsByLoginIdAndPropCdAndCmpxCd(String loginId, String propCd, String cmpxCd);
}
