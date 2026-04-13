package com.daol.concierge.core.api;

import java.util.HashMap;
import java.util.Map;

/**
 * 공통 응답 포맷: { resCd, resMsg, map }
 */
public class ApiResponse {

	private String resCd;
	private String resMsg;
	private Map<String, Object> map;

	public ApiResponse() {
		this.resCd = "0000";
		this.resMsg = "SUCCESS";
		this.map = new HashMap<>();
	}

	public ApiResponse(String resCd, String resMsg) {
		this.resCd = resCd;
		this.resMsg = resMsg;
		this.map = new HashMap<>();
	}

	public String getResCd() { return resCd; }
	public String getResMsg() { return resMsg; }
	public Map<String, Object> getMap() { return map; }

	public void setResCd(String resCd) { this.resCd = resCd; }
	public void setResMsg(String resMsg) { this.resMsg = resMsg; }
	public void setMap(Map<String, Object> map) { this.map = map; }
}
