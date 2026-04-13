package com.daol.concierge.gr.service;

import com.daol.concierge.core.api.BizException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 투숙객 요청 서비스 (인메모리 Mock - 서버 재시작 시 초기화)
 */
@Service
public class GrService {

	private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter FMT_TM = DateTimeFormatter.ofPattern("HHmm");

	// 예약 마스터
	private final Map<String, Map<String, Object>> reservations = new HashMap<>();
	// 어메니티 품목 마스터
	private final Map<String, Map<String, Object>> amenityItems = new LinkedHashMap<>();

	// 요청 저장소
	private final List<Map<String, Object>> amenityRequests = new ArrayList<>();
	private final List<Map<String, Object>> housekeepingRequests = new ArrayList<>();
	private final List<Map<String, Object>> lateCheckoutRequests = new ArrayList<>();

	public GrService() {
		// 예약 샘플 시드
		String[][] rsvRows = {
				{"R2026041300001", "1205", "HONG GILDONG", "20260413", "20260415", "1100", "DLX", "ko_KR"},
				{"R2026041300002", "0807", "JOHN SMITH",   "20260413", "20260414", "1100", "STD", "en_US"}
		};
		for (String[] row : rsvRows) {
			Map<String, Object> r = new LinkedHashMap<>();
			r.put("rsvNo", row[0]);
			r.put("roomNo", row[1]);
			r.put("perNm", row[2]);
			r.put("chkInDt", row[3]);
			r.put("chkOutDt", row[4]);
			r.put("chkOutTm", row[5]);
			r.put("roomTpCd", row[6]);
			r.put("perUseLang", row[7]);
			reservations.put(row[0], r);
		}

		// 어메니티 품목 시드
		Object[][] itemRows = {
				{"AM001", "수건",     "Towel",          4},
				{"AM002", "생수",     "Mineral Water",  6},
				{"AM003", "비누",     "Soap",           3},
				{"AM004", "샴푸",     "Shampoo",        3},
				{"AM005", "칫솔세트", "Toothbrush Set", 4}
		};
		for (Object[] row : itemRows) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("itemCd", row[0]);
			item.put("itemNm", row[1]);
			item.put("itemNmEng", row[2]);
			item.put("maxQty", row[3]);
			amenityItems.put((String) row[0], item);
		}
	}

	// ==================== 어메니티 ====================

	public List<Map<String, Object>> getAmenityItemList() {
		return new ArrayList<>(amenityItems.values());
	}

	public List<Map<String, Object>> getAmenityList(String rsvNo) {
		if (rsvNo == null || rsvNo.isEmpty()) {
			return new ArrayList<>(amenityRequests);
		}
		return amenityRequests.stream()
				.filter(r -> rsvNo.equals(r.get("rsvNo")))
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> insertAmenityReq(Map<String, Object> params) {
		String rsvNo = (String) params.get("rsvNo");
		String roomNo = (String) params.get("roomNo");
		List<Map<String, Object>> itemList = (List<Map<String, Object>>) params.get("itemList");
		String reqMemo = (String) params.getOrDefault("reqMemo", "");

		if (rsvNo == null || roomNo == null || itemList == null || itemList.isEmpty()) {
			throw new BizException("9001", "필수값 누락");
		}
		if (!reservations.containsKey(rsvNo)) {
			throw new BizException("9404", "예약 없음");
		}
		for (Map<String, Object> it : itemList) {
			String itemCd = (String) it.get("itemCd");
			Map<String, Object> master = amenityItems.get(itemCd);
			if (master == null) {
				throw new BizException("9002", "품목코드 오류: " + itemCd);
			}
			int qty = ((Number) it.get("qty")).intValue();
			int maxQty = ((Number) master.get("maxQty")).intValue();
			if (qty > maxQty) {
				throw new BizException("9003", "최대수량 초과: " + master.get("itemNm"));
			}
		}

		LocalDateTime now = LocalDateTime.now();
		String reqNo = "AM" + System.currentTimeMillis();
		Map<String, Object> record = new LinkedHashMap<>();
		record.put("reqNo", reqNo);
		record.put("rsvNo", rsvNo);
		record.put("roomNo", roomNo);
		record.put("itemList", itemList);
		record.put("reqMemo", reqMemo);
		record.put("procStatCd", "REQ");
		record.put("procStatNm", "접수");
		record.put("reqDt", now.format(FMT_DT));
		record.put("reqTm", now.format(FMT_TM));
		amenityRequests.add(record);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("procStatCd", "REQ");
		res.put("estArrMin", 15);
		return res;
	}

	// ==================== 하우스키핑 ====================

	public Map<String, Object> getHousekeepingStat(String rsvNo) {
		if (!reservations.containsKey(rsvNo)) {
			throw new BizException("9404", "예약 없음");
		}
		Map<String, Object> rsv = reservations.get(rsvNo);
		Map<String, Object> latest = null;
		for (int i = housekeepingRequests.size() - 1; i >= 0; i--) {
			if (rsvNo.equals(housekeepingRequests.get(i).get("rsvNo"))) {
				latest = housekeepingRequests.get(i);
				break;
			}
		}

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("rsvNo", rsvNo);
		res.put("roomNo", rsv.get("roomNo"));
		res.put("hkStatCd", latest != null ? latest.get("hkStatCd") : "CLR");
		res.put("hkStatNm", latest != null ? latest.get("hkStatNm") : "해제");
		res.put("lastReqDt", latest != null ? latest.get("reqDt") : null);
		res.put("lastReqTm", latest != null ? latest.get("reqTm") : null);
		return res;
	}

	public Map<String, Object> updateHousekeepingStat(Map<String, Object> params) {
		String rsvNo = (String) params.get("rsvNo");
		String hkStatCd = (String) params.get("hkStatCd");
		String reqMemo = (String) params.getOrDefault("reqMemo", "");

		if (rsvNo == null || hkStatCd == null) {
			throw new BizException("9001", "필수값 누락");
		}

		Map<String, String> statNmMap = new HashMap<>();
		statNmMap.put("MU", "객실정비요청");
		statNmMap.put("DND", "방해금지");
		statNmMap.put("CLR", "해제");
		if (!statNmMap.containsKey(hkStatCd)) {
			throw new BizException("9002", "상태코드 오류");
		}
		if (!reservations.containsKey(rsvNo)) {
			throw new BizException("9404", "예약 없음");
		}

		Map<String, Object> rsv = reservations.get(rsvNo);
		LocalDateTime now = LocalDateTime.now();
		String reqNo = "HK" + System.currentTimeMillis();

		Map<String, Object> record = new LinkedHashMap<>();
		record.put("reqNo", reqNo);
		record.put("rsvNo", rsvNo);
		record.put("roomNo", rsv.get("roomNo"));
		record.put("hkStatCd", hkStatCd);
		record.put("hkStatNm", statNmMap.get(hkStatCd));
		record.put("reqMemo", reqMemo);
		record.put("reqDt", now.format(FMT_DT));
		record.put("reqTm", now.format(FMT_TM));
		housekeepingRequests.add(record);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("hkStatCd", hkStatCd);
		res.put("hkStatNm", statNmMap.get(hkStatCd));
		return res;
	}

	// ==================== 레이트 체크아웃 ====================

	public Map<String, Object> getLateCheckoutInfo(String rsvNo, String reqOutTm) {
		if (rsvNo == null || reqOutTm == null) {
			throw new BizException("9001", "필수값 누락");
		}
		if (!reservations.containsKey(rsvNo)) {
			throw new BizException("9404", "예약 없음");
		}

		Map<String, Object> rsv = reservations.get(rsvNo);
		String chkOutTm = (String) rsv.get("chkOutTm");
		int curOutH = Integer.parseInt(chkOutTm.substring(0, 2));
		int reqH = Integer.parseInt(reqOutTm.substring(0, 2));
		int diffH = reqH - curOutH;

		String availYn = "Y";
		int addAmt = 0;
		String rateTpCd = "FREE";
		String rateTpNm = "무료";

		if (diffH <= 0) {
			availYn = "N"; rateTpCd = "NONE"; rateTpNm = "불가";
		} else if (diffH <= 2) {
			addAmt = 0;      rateTpCd = "FREE"; rateTpNm = "2시간 이내 무료";
		} else if (diffH <= 5) {
			addAmt = 50000;  rateTpCd = "HALF"; rateTpNm = "반일 요금";
		} else if (diffH <= 8) {
			addAmt = 100000; rateTpCd = "FULL"; rateTpNm = "전일 요금";
		} else {
			availYn = "N"; rateTpCd = "NONE"; rateTpNm = "연장 불가";
		}

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("rsvNo", rsvNo);
		res.put("roomNo", rsv.get("roomNo"));
		res.put("curChkOutTm", chkOutTm);
		res.put("reqChkOutTm", reqOutTm);
		res.put("availYn", availYn);
		res.put("addAmt", addAmt);
		res.put("curCd", "KRW");
		res.put("rateTpCd", rateTpCd);
		res.put("rateTpNm", rateTpNm);
		return res;
	}

	public Map<String, Object> insertLateCheckoutReq(Map<String, Object> params) {
		String rsvNo = (String) params.get("rsvNo");
		String reqOutTm = (String) params.get("reqOutTm");
		int addAmt = params.get("addAmt") == null ? 0 : ((Number) params.get("addAmt")).intValue();

		if (rsvNo == null || reqOutTm == null) {
			throw new BizException("9001", "필수값 누락");
		}
		if (!reservations.containsKey(rsvNo)) {
			throw new BizException("9404", "예약 없음");
		}

		Map<String, Object> rsv = reservations.get(rsvNo);
		LocalDateTime now = LocalDateTime.now();
		String reqNo = "LC" + System.currentTimeMillis();

		Map<String, Object> record = new LinkedHashMap<>();
		record.put("reqNo", reqNo);
		record.put("rsvNo", rsvNo);
		record.put("roomNo", rsv.get("roomNo"));
		record.put("curChkOutTm", rsv.get("chkOutTm"));
		record.put("reqChkOutTm", reqOutTm);
		record.put("addAmt", addAmt);
		record.put("procStatCd", "REQ");
		record.put("procStatNm", "접수");
		record.put("reqDt", now.format(FMT_DT));
		record.put("reqTm", now.format(FMT_TM));
		lateCheckoutRequests.add(record);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("procStatCd", "REQ");
		res.put("aprUserNm", "FRONT DESK");
		return res;
	}
}
