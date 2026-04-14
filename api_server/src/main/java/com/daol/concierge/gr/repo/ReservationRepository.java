package com.daol.concierge.gr.repo;

import com.daol.concierge.gr.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
	List<Reservation> findByPropCdOrderByChkInDtDesc(String propCd);
}
