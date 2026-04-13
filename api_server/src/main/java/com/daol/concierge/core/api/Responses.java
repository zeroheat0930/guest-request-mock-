package com.daol.concierge.core.api;

import java.util.List;
import java.util.Map;

/**
 * PMS 스타일 응답 빌더
 *   - Responses.MapResponse.of(map)
 *   - Responses.ListResponse.of(list)
 *   - Responses.ok() / Responses.fail(cd, msg)
 */
public class Responses {

	public static class MapResponse extends ApiResponse {
		public static MapResponse of(Map<String, Object> data) {
			MapResponse r = new MapResponse();
			if (data != null) {
				r.getMap().putAll(data);
			}
			return r;
		}
	}

	public static class ListResponse extends ApiResponse {
		public static ListResponse of(List<?> list) {
			ListResponse r = new ListResponse();
			r.getMap().put("totCnt", list == null ? 0 : list.size());
			r.getMap().put("list", list);
			return r;
		}
	}

	public static ApiResponse ok() {
		return new ApiResponse();
	}

	public static ApiResponse fail(String resCd, String resMsg) {
		return new ApiResponse(resCd, resMsg);
	}
}
