package com.daol.concierge.nearby;

/**
 * 주변 장소 DTO (카테고리별 검색 결과 1건)
 */
public record NearbyPlace(
		String name,
		String category,
		String categoryCd,
		Integer distanceM,
		String phone,
		String address,
		String mapUrl,
		String thumbnailUrl
) {}
