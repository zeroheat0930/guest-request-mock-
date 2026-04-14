package com.daol.concierge.gr.repo;

import com.daol.concierge.gr.domain.AmenityRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRequestRepository extends JpaRepository<AmenityRequest, String> {
	List<AmenityRequest> findByPropCdOrderByReqDtDescReqTmDesc(String propCd);
	List<AmenityRequest> findByPropCdAndRsvNoOrderByReqDtDescReqTmDesc(String propCd, String rsvNo);
}
