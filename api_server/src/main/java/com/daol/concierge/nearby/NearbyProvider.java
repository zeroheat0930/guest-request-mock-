package com.daol.concierge.nearby;

import java.util.List;

/**
 * 주변 장소 검색 추상화.
 *
 * 구현체는 `nearby.provider` 설정(mock | kakao) 으로 선택.
 * 실패 시 예외 대신 빈 리스트를 반환해 UI 는 "결과 없음" 으로 렌더한다.
 */
public interface NearbyProvider {

	List<NearbyPlace> search(double lat, double lng, String categoryCd, int radiusM);
}
