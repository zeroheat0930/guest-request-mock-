package com.daol.concierge.gr.service;

import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.BizException;
import com.daol.concierge.dispatcher.RequestDispatcher;
import com.daol.concierge.dispatcher.RequestEvent;
import com.daol.concierge.inv.mapper.InvMapper;
import com.daol.concierge.pms.mapper.PmsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GrService {

	@Autowired private PmsMapper pmsMapper;
	@Autowired private InvMapper invMapper;
	@Autowired private List<RequestDispatcher> requestDispatchers;

	private static String pp() { return SecurityContextUtil.requirePrincipal().propCd(); }
	private static String pc() { return SecurityContextUtil.requirePrincipal().cmpxCd(); }

	// ==================== 예약 ====================

	public Map<String, Object> getReservation(String rsvNo) {
		if (rsvNo == null || rsvNo.isEmpty()) throw new BizException("9001", "필수값 누락");
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Map<String, Object> r = pmsMapper.selectReservation(pp(), pc(), rsvNo);
		if (r == null) throw new BizException("9404", "예약 없음");
		return toResvMap(r);
	}

	public List<Map<String, Object>> getReservationList() {
		var principal = SecurityContextUtil.requirePrincipal();
		List<Map<String, Object>> out = new ArrayList<>();
		Map<String, Object> r = pmsMapper.selectReservation(pp(), pc(), principal.rsvNo());
		if (r != null) out.add(toResvMap(r));
		return out;
	}

	// ==================== 어메니티 ====================

	public List<Map<String, Object>> getAmenityItemList() {
		SecurityContextUtil.requirePrincipal();
		return invMapper.selectAmenityItems(pp(), pc());
	}

	public List<Map<String, Object>> getAmenityList(String rsvNo) {
		var principal = SecurityContextUtil.requirePrincipal();
		String target = (rsvNo == null || rsvNo.isEmpty()) ? principal.rsvNo() : rsvNo;
		if (!target.equals(principal.rsvNo())) throw new BizException("9102", "권한 없음");
		return invMapper.selectAmenityReqList(pp(), pc(), target);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> insertAmenityReq(Map<String, Object> params) {
		String rsvNo = str(params.get("rsvNo"));
		String roomNo = str(params.get("roomNo"));
		List<Map<String, Object>> itemList = (List<Map<String, Object>>) params.get("itemList");
		String reqMemo = str(params.getOrDefault("reqMemo", ""));

		if (rsvNo == null || roomNo == null || itemList == null || itemList.isEmpty())
			throw new BizException("9001", "필수값 누락");
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		if (pmsMapper.selectReservation(pp(), pc(), rsvNo) == null)
			throw new BizException("9404", "예약 없음");

		List<Long> reqNos = new ArrayList<>();
		for (Map<String, Object> it : itemList) {
			String itemCd = str(it.get("itemCd"));
			int qty = it.get("qty") instanceof Number n ? n.intValue() : 1;

			Map<String, Object> row = new HashMap<>();
			row.put("propCd", pp());
			row.put("cmpxCd", pc());
			row.put("resvNo", rsvNo);
			row.put("rmNo", roomNo);
			row.put("itemCd", itemCd);
			row.put("qty", qty);
			row.put("memo", reqMemo);
			row.put("regUser", rsvNo);
			invMapper.insertAmenityReq(row);
			Object generated = row.get("reqNo");
			if (generated instanceof Number n) reqNos.add(n.longValue());
		}

		String reqNoStr = reqNos.isEmpty() ? "AM" + System.currentTimeMillis() : String.valueOf(reqNos.get(0));
		RequestEvent ev = new RequestEvent(pp(), pc(), "AMENITY",
				"[" + roomNo + "] 어메니티 요청 " + itemList.size() + "건", roomNo, reqNoStr, reqMemo);
		requestDispatchers.forEach(d -> d.dispatch(ev));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNoStr);
		res.put("procStatCd", "REQ");
		res.put("estArrMin", 15);
		return res;
	}

	// ==================== 하우스키핑 ====================

	public Map<String, Object> getHousekeepingStat(String rsvNo) {
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Map<String, Object> rsv = pmsMapper.selectReservation(pp(), pc(), rsvNo);
		if (rsv == null) throw new BizException("9404", "예약 없음");

		Map<String, Object> latest = invMapper.selectLatestHkTask(pp(), pc(), str(rsv.get("rmNo")));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("rsvNo", rsvNo);
		res.put("roomNo", str(rsv.get("rmNo")));
		if (latest != null) {
			String srcType = str(latest.get("sourceType"));
			String hkCd = srcType != null && srcType.startsWith("HK_") ? srcType.substring(3) : "CLR";
			res.put("hkStatCd", hkCd);
			res.put("hkStatNm", hkStatNm(hkCd));
			res.put("lastReqDt", str(latest.get("createdAt")));
		} else {
			res.put("hkStatCd", "CLR");
			res.put("hkStatNm", "해제");
			res.put("lastReqDt", null);
		}
		return res;
	}

	public Map<String, Object> updateHousekeepingStat(Map<String, Object> params) {
		String rsvNo = str(params.get("rsvNo"));
		String hkStatCd = str(params.get("hkStatCd"));
		String reqMemo = str(params.getOrDefault("reqMemo", ""));

		if (rsvNo == null || hkStatCd == null) throw new BizException("9001", "필수값 누락");
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		String nm = hkStatNm(hkStatCd);
		if (nm == null) throw new BizException("9002", "상태코드 오류");

		Map<String, Object> rsv = pmsMapper.selectReservation(pp(), pc(), rsvNo);
		if (rsv == null) throw new BizException("9404", "예약 없음");
		String roomNo = str(rsv.get("rmNo"));

		String taskId = "HK" + System.currentTimeMillis();
		Map<String, Object> task = new HashMap<>();
		task.put("taskId", taskId);
		task.put("propCd", pp());
		task.put("cmpxCd", pc());
		task.put("deptCd", "HK");
		task.put("sourceType", "HK_" + hkStatCd);
		task.put("sourceRefNo", rsvNo);
		task.put("title", "[" + roomNo + "] 객실정비 " + nm);
		task.put("memo", reqMemo);
		task.put("rmNo", roomNo);
		task.put("assigneeId", null);
		task.put("statusCd", "REQ");
		invMapper.insertTask(task);

		RequestEvent ev = new RequestEvent(pp(), pc(), "HK_" + hkStatCd,
				"[" + roomNo + "] 객실정비 " + nm, roomNo, taskId, reqMemo);
		requestDispatchers.forEach(d -> d.dispatch(ev));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", taskId);
		res.put("hkStatCd", hkStatCd);
		res.put("hkStatNm", nm);
		return res;
	}

	// ==================== 레이트 체크아웃 ====================

	public Map<String, Object> getLateCheckoutInfo(String rsvNo, String reqOutTm) {
		if (rsvNo == null || reqOutTm == null) throw new BizException("9001", "필수값 누락");
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Map<String, Object> rsv = pmsMapper.selectReservation(pp(), pc(), rsvNo);
		if (rsv == null) throw new BizException("9404", "예약 없음");

		String depHour = str(rsv.get("depHour"));
		if (depHour == null || depHour.isEmpty()) depHour = "1100";
		int curOutH = Integer.parseInt(depHour.substring(0, 2));
		int reqH = Integer.parseInt(reqOutTm.substring(0, 2));
		int diffH = reqH - curOutH;

		String availYn, rateTpCd, rateTpNm;
		int addAmt;
		if (diffH <= 0) { availYn="N"; addAmt=0; rateTpCd="NONE"; rateTpNm="불가"; }
		else if (diffH <= 2) { availYn="Y"; addAmt=0; rateTpCd="FREE"; rateTpNm="2시간 이내 무료"; }
		else if (diffH <= 5) { availYn="Y"; addAmt=50000; rateTpCd="HALF"; rateTpNm="반일 요금"; }
		else if (diffH <= 8) { availYn="Y"; addAmt=100000; rateTpCd="FULL"; rateTpNm="전일 요금"; }
		else { availYn="N"; addAmt=0; rateTpCd="NONE"; rateTpNm="연장 불가"; }

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("rsvNo", rsvNo);
		res.put("roomNo", str(rsv.get("rmNo")));
		res.put("curChkOutTm", depHour);
		res.put("reqChkOutTm", reqOutTm);
		res.put("availYn", availYn);
		res.put("addAmt", addAmt);
		res.put("curCd", "KRW");
		res.put("rateTpCd", rateTpCd);
		res.put("rateTpNm", rateTpNm);
		return res;
	}

	public Map<String, Object> insertLateCheckoutReq(Map<String, Object> params) {
		String rsvNo = str(params.get("rsvNo"));
		String reqOutTm = str(params.get("reqOutTm"));
		int addAmt = params.get("addAmt") == null ? 0 : ((Number) params.get("addAmt")).intValue();

		if (rsvNo == null || reqOutTm == null) throw new BizException("9001", "필수값 누락");
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Map<String, Object> rsv = pmsMapper.selectReservation(pp(), pc(), rsvNo);
		if (rsv == null) throw new BizException("9404", "예약 없음");
		String roomNo = str(rsv.get("rmNo"));

		Map<String, Object> row = new HashMap<>();
		row.put("propCd", pp());
		row.put("cmpxCd", pc());
		row.put("resvNo", rsvNo);
		row.put("rmNo", roomNo);
		row.put("reqOutTm", reqOutTm);
		row.put("addAmt", addAmt);
		row.put("curCd", "KRW");
		row.put("rateTpCd", null);
		row.put("regUser", rsvNo);
		invMapper.insertLateCo(row);

		String reqNo = row.get("reqNo") != null ? String.valueOf(row.get("reqNo")) : "LC" + System.currentTimeMillis();
		RequestEvent ev = new RequestEvent(pp(), pc(), "LATE_CO",
				"[" + roomNo + "] 레이트체크아웃 " + reqOutTm + " +" + addAmt + "원", roomNo, reqNo, null);
		requestDispatchers.forEach(d -> d.dispatch(ev));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("procStatCd", "REQ");
		res.put("aprUserNm", "FRONT DESK");
		return res;
	}

	// ==================== 주차 ====================

	public List<Map<String, Object>> getParkingList(String rsvNo) {
		var principal = SecurityContextUtil.requirePrincipal();
		String target = (rsvNo == null || rsvNo.isEmpty()) ? principal.rsvNo() : rsvNo;
		SecurityContextUtil.assertOwnsRsv(target);
		return invMapper.selectParkingList(pp(), pc(), target);
	}

	public Map<String, Object> insertParkingReq(Map<String, Object> params) {
		String rsvNo = str(params.get("rsvNo"));
		String roomNo = str(params.get("roomNo"));
		String carNo = str(params.get("carNo"));
		String carTp = str(params.getOrDefault("carTp", ""));
		String reqMemo = str(params.getOrDefault("reqMemo", ""));

		if (carNo == null || carNo.trim().isEmpty()) throw new BizException("9001", "차량번호 누락");
		carNo = carNo.trim();
		if (carNo.length() < 4 || carNo.length() > 20) throw new BizException("9002", "차량번호 형식 오류");
		if (rsvNo == null || roomNo == null) throw new BizException("9001", "필수값 누락");
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		if (pmsMapper.selectReservation(pp(), pc(), rsvNo) == null)
			throw new BizException("9404", "예약 없음");

		Map<String, Object> row = new HashMap<>();
		row.put("propCd", pp());
		row.put("cmpxCd", pc());
		row.put("resvNo", rsvNo);
		row.put("rmNo", roomNo);
		row.put("carNo", carNo);
		row.put("carTp", carTp);
		row.put("memo", reqMemo);
		row.put("regUser", rsvNo);
		invMapper.insertParking(row);

		String reqNo = row.get("reqNo") != null ? String.valueOf(row.get("reqNo")) : "PK" + System.currentTimeMillis();
		RequestEvent ev = new RequestEvent(pp(), pc(), "PARKING",
				"[" + roomNo + "] 차량 등록 " + carNo, roomNo, reqNo, reqMemo);
		requestDispatchers.forEach(d -> d.dispatch(ev));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("procStatCd", "REQ");
		res.put("carNo", carNo);
		return res;
	}

	// ==================== 헬퍼 ====================

	private Map<String, Object> toResvMap(Map<String, Object> r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("rsvNo", str(r.get("resvNo")));
		m.put("roomNo", str(r.get("rmNo")));
		m.put("perNm", str(r.get("perNm")));
		m.put("chkInDt", str(r.get("arrDt")));
		m.put("chkOutDt", str(r.get("depDt")));
		m.put("chkOutTm", str(r.get("depHour")));
		m.put("roomTpCd", str(r.get("rmTpCd")));
		m.put("perUseLang", str(r.get("perUseLang")));
		return m;
	}

	private String hkStatNm(String cd) {
		if ("MU".equals(cd)) return "객실정비요청";
		if ("DND".equals(cd)) return "방해금지";
		if ("CLR".equals(cd)) return "해제";
		return null;
	}

	private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
