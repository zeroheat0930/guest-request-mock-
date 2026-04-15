package com.daol.concierge.gr.service;

import com.daol.concierge.auth.SecurityContextUtil;
import com.daol.concierge.core.api.BizException;
import com.daol.concierge.gr.domain.AmenityItem;
import com.daol.concierge.gr.domain.AmenityRequest;
import com.daol.concierge.gr.domain.AmenityRequestItem;
import com.daol.concierge.gr.domain.HousekeepingRequest;
import com.daol.concierge.gr.domain.LateCheckoutRequest;
import com.daol.concierge.gr.domain.ParkingRegistration;
import com.daol.concierge.gr.domain.Reservation;
import com.daol.concierge.gr.repo.AmenityItemRepository;
import com.daol.concierge.gr.repo.AmenityRequestRepository;
import com.daol.concierge.gr.repo.HousekeepingRequestRepository;
import com.daol.concierge.gr.repo.LateCheckoutRequestRepository;
import com.daol.concierge.gr.repo.ParkingRegistrationRepository;
import com.daol.concierge.gr.repo.ReservationRepository;
import com.daol.concierge.dispatcher.RequestDispatcher;
import com.daol.concierge.dispatcher.RequestEvent;
import com.daol.concierge.pms.PmsCarRegistryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 투숙객 요청 서비스 (JPA 기반)
 *
 * US-003 에서 인메모리 Map/List → Spring Data JPA 로 전환.
 * US-004/005 이후: 모든 쿼리/삽입이 SecurityContext 의 게스트 principal 에서 rsvNo + propCd 를 꺼내
 *   "본인 것만" 보이도록 강제. 체인 단위 격리는 principalPropCd() 헬퍼로 관리.
 * 응답 Map 의 key 이름은 PMS 스타일(camelCase 약어) 그대로 유지 → 프론트 호환성 확보.
 */
@Service
public class GrService {

	private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter FMT_TM = DateTimeFormatter.ofPattern("HHmm");
	/** 현재 인증된 게스트의 propCd 를 꺼낸다 (멀티 프로퍼티 격리 US-005). */
	private static String principalPropCd() {
		return SecurityContextUtil.requirePrincipal().propCd();
	}

	@Autowired private ReservationRepository reservationRepo;
	@Autowired private AmenityItemRepository amenityItemRepo;
	@Autowired private AmenityRequestRepository amenityRequestRepo;
	@Autowired private HousekeepingRequestRepository housekeepingRepo;
	@Autowired private LateCheckoutRequestRepository lateCheckoutRepo;
	@Autowired private ParkingRegistrationRepository parkingRepo;
	@Autowired private RequestDispatcher requestDispatcher;
	@Autowired(required = false) private PmsCarRegistryAdapter pmsCarRegistryAdapter;

	// ==================== 예약 ====================

	@Transactional(readOnly = true)
	public Map<String, Object> getReservation(String rsvNo) {
		if (rsvNo == null || rsvNo.isEmpty()) {
			throw new BizException("9001", "필수값 누락");
		}
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Reservation r = reservationRepo.findById(rsvNo)
				.orElseThrow(() -> new BizException("9404", "예약 없음"));
		return reservationToMap(r);
	}

	/**
	 * 인증된 게스트 본인의 예약만 반환 (프론트가 "내 예약" 단건만 봐야 함).
	 * 복수 예약이 있는 경우 같은 propCd + 같은 rsvNo 로 1건만 매칭.
	 */
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getReservationList() {
		var principal = SecurityContextUtil.requirePrincipal();
		List<Map<String, Object>> out = new ArrayList<>();
		reservationRepo.findById(principal.rsvNo()).ifPresent(r -> out.add(reservationToMap(r)));
		return out;
	}

	// ==================== 어메니티 ====================

	/**
	 * 어메니티 품목 마스터는 체인 전체가 공유하는 공통 카탈로그로 취급 → propCd 필터 없음.
	 * (개별 프로퍼티가 독자 품목을 가지는 요구가 생기면 그때 propCd 필터로 바꾸면 됨.)
	 */
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getAmenityItemList() {
		SecurityContextUtil.requirePrincipal(); // 인증만 요구, propCd 필터는 안 쓴다
		List<Map<String, Object>> out = new ArrayList<>();
		for (AmenityItem it : amenityItemRepo.findAll()) {
			out.add(amenityItemToMap(it));
		}
		return out;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> getAmenityList(String rsvNo) {
		// rsvNo 가 없으면 인증된 본인 것, 있으면 본인 것인지 확인
		var principal = SecurityContextUtil.requirePrincipal();
		String target = (rsvNo == null || rsvNo.isEmpty()) ? principal.rsvNo() : rsvNo;
		if (!target.equals(principal.rsvNo())) {
			throw new BizException("9102", "권한 없음");
		}
		List<AmenityRequest> rows =
				amenityRequestRepo.findByPropCdAndRsvNoOrderByReqDtDescReqTmDesc(principalPropCd(), target);
		List<Map<String, Object>> out = new ArrayList<>();
		for (AmenityRequest r : rows) {
			out.add(amenityRequestToMap(r));
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public Map<String, Object> insertAmenityReq(Map<String, Object> params) {
		String rsvNo = (String) params.get("rsvNo");
		String roomNo = (String) params.get("roomNo");
		List<Map<String, Object>> itemList = (List<Map<String, Object>>) params.get("itemList");
		String reqMemo = (String) params.getOrDefault("reqMemo", "");

		if (rsvNo == null || roomNo == null || itemList == null || itemList.isEmpty()) {
			throw new BizException("9001", "필수값 누락");
		}
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		if (reservationRepo.findById(rsvNo).isEmpty()) {
			throw new BizException("9404", "예약 없음");
		}

		AmenityRequest req = new AmenityRequest();
		req.setPropCd(principalPropCd());
		req.setRsvNo(rsvNo);
		req.setRoomNo(roomNo);
		req.setReqMemo(reqMemo);
		req.setProcStatCd("REQ");

		for (Map<String, Object> it : itemList) {
			String itemCd = (String) it.get("itemCd");
			AmenityItem master = amenityItemRepo.findById(itemCd)
					.orElseThrow(() -> new BizException("9002", "품목코드 오류: " + itemCd));
			Object qtyObj = it.get("qty");
			if (!(qtyObj instanceof Number)) {
				throw new BizException("9001", "qty 필수");
			}
			int qty = ((Number) qtyObj).intValue();
			if (qty > master.getMaxQty()) {
				throw new BizException("9003", "최대수량 초과: " + master.getItemNm());
			}
			req.getItems().add(new AmenityRequestItem(itemCd, qty));
		}

		LocalDateTime now = LocalDateTime.now();
		String reqNo = "AM" + System.currentTimeMillis();
		req.setReqNo(reqNo);
		req.setReqDt(now.format(FMT_DT));
		req.setReqTm(now.format(FMT_TM));
		amenityRequestRepo.save(req);

		requestDispatcher.dispatch(new RequestEvent(
				principalPropCd(),
				"AMENITY",
				"[" + roomNo + "] 어메니티 요청 " + req.getItems().size() + "건 (" + reqNo + ")",
				roomNo,
				reqNo,
				reqMemo
		));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("procStatCd", "REQ");
		res.put("estArrMin", 15);
		return res;
	}

	// ==================== 하우스키핑 ====================

	@Transactional(readOnly = true)
	public Map<String, Object> getHousekeepingStat(String rsvNo) {
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Reservation rsv = reservationRepo.findById(rsvNo)
				.orElseThrow(() -> new BizException("9404", "예약 없음"));

		Optional<HousekeepingRequest> latest = housekeepingRepo
				.findFirstByPropCdAndRsvNoOrderByReqDtDescReqTmDesc(principalPropCd(), rsvNo);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("rsvNo", rsvNo);
		res.put("roomNo", rsv.getRoomNo());
		if (latest.isPresent()) {
			HousekeepingRequest hk = latest.get();
			res.put("hkStatCd", hk.getHkStatCd());
			res.put("hkStatNm", hkStatNm(hk.getHkStatCd()));
			res.put("lastReqDt", hk.getReqDt());
			res.put("lastReqTm", hk.getReqTm());
		} else {
			res.put("hkStatCd", "CLR");
			res.put("hkStatNm", "해제");
			res.put("lastReqDt", null);
			res.put("lastReqTm", null);
		}
		return res;
	}

	@Transactional
	public Map<String, Object> updateHousekeepingStat(Map<String, Object> params) {
		String rsvNo = (String) params.get("rsvNo");
		String hkStatCd = (String) params.get("hkStatCd");
		String reqMemo = (String) params.getOrDefault("reqMemo", "");

		if (rsvNo == null || hkStatCd == null) {
			throw new BizException("9001", "필수값 누락");
		}
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		String nm = hkStatNm(hkStatCd);
		if (nm == null) {
			throw new BizException("9002", "상태코드 오류");
		}

		Reservation rsv = reservationRepo.findById(rsvNo)
				.orElseThrow(() -> new BizException("9404", "예약 없음"));

		LocalDateTime now = LocalDateTime.now();
		String reqNo = "HK" + System.currentTimeMillis();

		HousekeepingRequest hk = new HousekeepingRequest();
		hk.setReqNo(reqNo);
		hk.setPropCd(principalPropCd());
		hk.setRsvNo(rsvNo);
		hk.setRoomNo(rsv.getRoomNo());
		hk.setHkStatCd(hkStatCd);
		hk.setReqMemo(reqMemo);
		hk.setReqDt(now.format(FMT_DT));
		hk.setReqTm(now.format(FMT_TM));
		housekeepingRepo.save(hk);

		requestDispatcher.dispatch(new RequestEvent(
				principalPropCd(),
				"HK_" + hkStatCd,
				"[" + rsv.getRoomNo() + "] 객실정비 " + nm + " (" + reqNo + ")",
				rsv.getRoomNo(),
				reqNo,
				reqMemo
		));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("hkStatCd", hkStatCd);
		res.put("hkStatNm", nm);
		return res;
	}

	// ==================== 레이트 체크아웃 ====================

	@Transactional(readOnly = true)
	public Map<String, Object> getLateCheckoutInfo(String rsvNo, String reqOutTm) {
		if (rsvNo == null || reqOutTm == null) {
			throw new BizException("9001", "필수값 누락");
		}
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Reservation rsv = reservationRepo.findById(rsvNo)
				.orElseThrow(() -> new BizException("9404", "예약 없음"));

		String chkOutTm = rsv.getChkOutTm();
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
		res.put("roomNo", rsv.getRoomNo());
		res.put("curChkOutTm", chkOutTm);
		res.put("reqChkOutTm", reqOutTm);
		res.put("availYn", availYn);
		res.put("addAmt", addAmt);
		res.put("curCd", "KRW");
		res.put("rateTpCd", rateTpCd);
		res.put("rateTpNm", rateTpNm);
		return res;
	}

	@Transactional
	public Map<String, Object> insertLateCheckoutReq(Map<String, Object> params) {
		String rsvNo = (String) params.get("rsvNo");
		String reqOutTm = (String) params.get("reqOutTm");
		int addAmt = params.get("addAmt") == null ? 0 : ((Number) params.get("addAmt")).intValue();

		if (rsvNo == null || reqOutTm == null) {
			throw new BizException("9001", "필수값 누락");
		}
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		Reservation rsv = reservationRepo.findById(rsvNo)
				.orElseThrow(() -> new BizException("9404", "예약 없음"));

		LocalDateTime now = LocalDateTime.now();
		String reqNo = "LC" + System.currentTimeMillis();

		LateCheckoutRequest lc = new LateCheckoutRequest();
		lc.setReqNo(reqNo);
		lc.setPropCd(principalPropCd());
		lc.setRsvNo(rsvNo);
		lc.setRoomNo(rsv.getRoomNo());
		lc.setCurChkOutTm(rsv.getChkOutTm());
		lc.setReqChkOutTm(reqOutTm);
		lc.setAddAmt(addAmt);
		lc.setProcStatCd("REQ");
		lc.setReqDt(now.format(FMT_DT));
		lc.setReqTm(now.format(FMT_TM));
		lateCheckoutRepo.save(lc);

		requestDispatcher.dispatch(new RequestEvent(
				principalPropCd(),
				"LATE_CO",
				"[" + rsv.getRoomNo() + "] 레이트체크아웃 " + reqOutTm + " +" + addAmt + "원 (" + reqNo + ")",
				rsv.getRoomNo(),
				reqNo,
				null
		));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("procStatCd", "REQ");
		res.put("aprUserNm", "FRONT DESK");
		return res;
	}

	// ==================== 주차 ====================

	@Transactional(readOnly = true)
	public List<Map<String, Object>> getParkingList(String rsvNo) {
		var principal = SecurityContextUtil.requirePrincipal();
		String target = (rsvNo == null || rsvNo.isEmpty()) ? principal.rsvNo() : rsvNo;
		SecurityContextUtil.assertOwnsRsv(target);
		List<ParkingRegistration> rows =
				parkingRepo.findByPropCdAndRsvNoOrderByReqDtDescReqTmDesc(principalPropCd(), target);
		List<Map<String, Object>> out = new ArrayList<>();
		for (ParkingRegistration r : rows) {
			out.add(parkingRegistrationToMap(r));
		}
		return out;
	}

	@Transactional
	public Map<String, Object> insertParkingReq(Map<String, Object> params) {
		String rsvNo = (String) params.get("rsvNo");
		String roomNo = (String) params.get("roomNo");
		String carNo = (String) params.get("carNo");
		String carTp = (String) params.getOrDefault("carTp", "");
		String reqMemo = (String) params.getOrDefault("reqMemo", "");

		if (carNo == null || carNo.trim().isEmpty()) {
			throw new BizException("9001", "차량번호 누락");
		}
		carNo = carNo.trim();
		if (carNo.length() < 4 || carNo.length() > 20) {
			throw new BizException("9002", "차량번호 형식 오류");
		}
		if (rsvNo == null || roomNo == null) {
			throw new BizException("9001", "필수값 누락");
		}
		SecurityContextUtil.assertOwnsRsv(rsvNo);
		if (reservationRepo.findById(rsvNo).isEmpty()) {
			throw new BizException("9404", "예약 없음");
		}

		LocalDateTime now = LocalDateTime.now();
		String reqNo = "PK" + System.currentTimeMillis();

		ParkingRegistration pk = new ParkingRegistration();
		pk.setReqNo(reqNo);
		pk.setPropCd(principalPropCd());
		pk.setRsvNo(rsvNo);
		pk.setRoomNo(roomNo);
		pk.setCarNo(carNo);
		pk.setCarTp(carTp);
		pk.setReqMemo(reqMemo);
		pk.setProcStatCd("REQ");
		pk.setReqDt(now.format(FMT_DT));
		pk.setReqTm(now.format(FMT_TM));
		parkingRepo.save(pk);

		if (pmsCarRegistryAdapter != null) {
			pmsCarRegistryAdapter.register(rsvNo, carNo);
		}

		requestDispatcher.dispatch(new RequestEvent(
				principalPropCd(),
				"PARKING",
				"[" + roomNo + "] 차량 등록 " + carNo + " (" + reqNo + ")",
				roomNo,
				reqNo,
				reqMemo
		));

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("reqNo", reqNo);
		res.put("procStatCd", "REQ");
		res.put("carNo", carNo);
		return res;
	}

	// ==================== 헬퍼 ====================

	private Map<String, Object> reservationToMap(Reservation r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("rsvNo", r.getRsvNo());
		m.put("roomNo", r.getRoomNo());
		m.put("perNm", r.getPerNm());
		m.put("chkInDt", r.getChkInDt());
		m.put("chkOutDt", r.getChkOutDt());
		m.put("chkOutTm", r.getChkOutTm());
		m.put("roomTpCd", r.getRoomTpCd());
		m.put("perUseLang", r.getPerUseLang());
		return m;
	}

	private Map<String, Object> amenityItemToMap(AmenityItem it) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("itemCd", it.getItemCd());
		m.put("itemNm", it.getItemNm());
		m.put("itemNmEng", it.getItemNmEng());
		m.put("maxQty", it.getMaxQty());
		return m;
	}

	private Map<String, Object> amenityRequestToMap(AmenityRequest r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("reqNo", r.getReqNo());
		m.put("rsvNo", r.getRsvNo());
		m.put("roomNo", r.getRoomNo());
		List<Map<String, Object>> items = new ArrayList<>();
		for (AmenityRequestItem it : r.getItems()) {
			Map<String, Object> im = new HashMap<>();
			im.put("itemCd", it.getItemCd());
			im.put("qty", it.getQty());
			items.add(im);
		}
		m.put("itemList", items);
		m.put("reqMemo", r.getReqMemo());
		m.put("procStatCd", r.getProcStatCd());
		m.put("procStatNm", "접수");
		m.put("reqDt", r.getReqDt());
		m.put("reqTm", r.getReqTm());
		return m;
	}

	private Map<String, Object> parkingRegistrationToMap(ParkingRegistration r) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("reqNo", r.getReqNo());
		m.put("rsvNo", r.getRsvNo());
		m.put("roomNo", r.getRoomNo());
		m.put("carNo", r.getCarNo());
		m.put("carTp", r.getCarTp());
		m.put("reqMemo", r.getReqMemo());
		m.put("procStatCd", r.getProcStatCd());
		m.put("procStatNm", procStatNm(r.getProcStatCd()));
		m.put("reqDt", r.getReqDt());
		m.put("reqTm", r.getReqTm());
		return m;
	}

	private String procStatNm(String cd) {
		if ("REQ".equals(cd)) return "접수";
		if ("APR".equals(cd)) return "승인";
		if ("CXL".equals(cd)) return "취소";
		return "접수";
	}

	private String hkStatNm(String hkStatCd) {
		if ("MU".equals(hkStatCd)) return "객실정비요청";
		if ("DND".equals(hkStatCd)) return "방해금지";
		if ("CLR".equals(hkStatCd)) return "해제";
		return null;
	}
}
