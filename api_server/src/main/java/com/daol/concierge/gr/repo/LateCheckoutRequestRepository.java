package com.daol.concierge.gr.repo;

import com.daol.concierge.gr.domain.LateCheckoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LateCheckoutRequestRepository extends JpaRepository<LateCheckoutRequest, String> {
}
