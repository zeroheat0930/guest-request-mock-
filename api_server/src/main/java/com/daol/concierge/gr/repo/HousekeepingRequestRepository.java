package com.daol.concierge.gr.repo;

import com.daol.concierge.gr.domain.HousekeepingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HousekeepingRequestRepository extends JpaRepository<HousekeepingRequest, String> {
	Optional<HousekeepingRequest> findFirstByPropCdAndRsvNoOrderByReqDtDescReqTmDesc(String propCd, String rsvNo);
}
