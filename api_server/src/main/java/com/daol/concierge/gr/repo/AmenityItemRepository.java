package com.daol.concierge.gr.repo;

import com.daol.concierge.gr.domain.AmenityItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityItemRepository extends JpaRepository<AmenityItem, String> {
	List<AmenityItem> findByPropCdOrderByItemCdAsc(String propCd);
}
