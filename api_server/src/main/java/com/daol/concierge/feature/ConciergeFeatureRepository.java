package com.daol.concierge.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConciergeFeatureRepository extends JpaRepository<ConciergeFeature, ConciergeFeatureId> {

	List<ConciergeFeature> findByPropCdOrderBySortOrdAsc(String propCd);
}
