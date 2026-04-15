package com.daol.concierge.gr.repo;

import com.daol.concierge.gr.domain.ParkingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingRegistrationRepository extends JpaRepository<ParkingRegistration, String> {
	List<ParkingRegistration> findByPropCdAndRsvNoOrderByReqDtDescReqTmDesc(String propCd, String rsvNo);
}
