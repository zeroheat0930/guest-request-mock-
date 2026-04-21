package com.daol.concierge.gr;

import com.daol.concierge.ccs.service.CcsLostFoundService;
import com.daol.concierge.ccs.service.CcsRentalService;
import com.daol.concierge.ccs.service.CcsVocService;
import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.gr.service.GrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 투숙객 요청(Guest Request) 컨트롤러
 */
@Tag(name = "Guest Request", description = "투숙객 앱 API (어메니티/하우스키핑/레이트체크아웃/주차/분실물/VOC/대여)")
@Controller
@RequestMapping(value = "/api/gr")
public class GrController extends BaseController {

	@Autowired
	private GrService grService;

	@Autowired(required = false) private CcsLostFoundService lostFoundService;
	@Autowired(required = false) private CcsVocService vocService;
	@Autowired(required = false) private CcsRentalService rentalService;

	@Value("${concierge.tenant.prop-cd:0000000001}") private String propCd;
	@Value("${concierge.tenant.cmpx-cd:00001}") private String cmpxCd;

	/**
	 * 예약 단건 조회 (챗봇이 perUseLang/roomNo 가져가는 용도)
	 */
	@ResponseBody
	@RequestMapping(value = "/reservation", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse getReservation(RequestParams requestParams) {
		return Responses.MapResponse.of(grService.getReservation(requestParams.getString("rsvNo")));
	}

	/**
	 * 예약 목록 조회 (대시보드 / 챗봇 예약 선택 드롭다운)
	 */
	@ResponseBody
	@RequestMapping(value = "/reservation/list", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse getReservationList() {
		return Responses.ListResponse.of(grService.getReservationList());
	}

	/**
	 * 어메니티 품목 마스터 조회
	 */
	@ResponseBody
	@RequestMapping(value = "/amenity/items", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse getAmenityItemList() {
		return Responses.ListResponse.of(grService.getAmenityItemList());
	}

	/**
	 * 어메니티 요청 목록 조회
	 * @param requestParams rsvNo 예약번호 (optional)
	 */
	@ResponseBody
	@RequestMapping(value = "/amenity/list", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse getAmenityList(RequestParams requestParams) {
		return Responses.ListResponse.of(grService.getAmenityList(requestParams.getString("rsvNo")));
	}

	/**
	 * 어메니티 요청 등록
	 */
	@ResponseBody
	@RequestMapping(value = "/amenity", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertAmenityReq(RequestParams requestParams) {
		return Responses.MapResponse.of(grService.insertAmenityReq(requestParams.getParams()));
	}

	/**
	 * 하우스키핑 상태 조회
	 */
	@ResponseBody
	@RequestMapping(value = "/housekeeping", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse getHousekeepingStat(RequestParams requestParams) {
		return Responses.MapResponse.of(grService.getHousekeepingStat(requestParams.getString("rsvNo")));
	}

	/**
	 * 하우스키핑 상태 변경 (MU=객실정비, DND=방해금지, CLR=해제)
	 */
	@ResponseBody
	@RequestMapping(value = "/housekeeping", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse updateHousekeepingStat(RequestParams requestParams) {
		return Responses.MapResponse.of(grService.updateHousekeepingStat(requestParams.getParams()));
	}

	/**
	 * 레이트 체크아웃 가능여부 및 요금 조회
	 */
	@ResponseBody
	@RequestMapping(value = "/late-checkout", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse getLateCheckoutInfo(RequestParams requestParams) {
		return Responses.MapResponse.of(grService.getLateCheckoutInfo(
				requestParams.getString("rsvNo"),
				requestParams.getString("reqOutTm")));
	}

	/**
	 * 레이트 체크아웃 신청
	 */
	@ResponseBody
	@RequestMapping(value = "/late-checkout", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertLateCheckoutReq(RequestParams requestParams) {
		return Responses.MapResponse.of(grService.insertLateCheckoutReq(requestParams.getParams()));
	}

	/**
	 * 주차 차량 등록 목록 조회
	 */
	@ResponseBody
	@RequestMapping(value = "/parking/list", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse getParkingList(RequestParams requestParams) {
		return Responses.ListResponse.of(grService.getParkingList(requestParams.getString("rsvNo")));
	}

	/**
	 * 주차 차량 등록
	 */
	@ResponseBody
	@RequestMapping(value = "/parking", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertParkingReq(RequestParams requestParams) {
		return Responses.MapResponse.of(grService.insertParkingReq(requestParams.getParams()));
	}

	/**
	 * 분실물 신고 (게스트) — Phase B.
	 * CCS_LOSTFOUND 에 REPORTER_TYPE='GUEST' 로 저장 후 FR 부서에 실시간 푸시.
	 */
	@ResponseBody
	@RequestMapping(value = "/lostfound", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertLostFound(RequestParams requestParams) {
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", propCd);
		body.putIfAbsent("cmpxCd", cmpxCd);
		body.put("reporterType", "GUEST");
		// rsvNo 를 reporterRef 로 설정 (게스트 신원)
		Object rsv = body.get("rsvNo");
		if (rsv != null) body.put("reporterRef", String.valueOf(rsv));
		Map<String, Object> saved = lostFoundService.createReport(body);
		return Responses.MapResponse.of(saved);
	}

	/**
	 * VOC(고객 불만) 접수 (게스트) — Phase B.
	 */
	@ResponseBody
	@RequestMapping(value = "/voc", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertVoc(RequestParams requestParams) {
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", propCd);
		body.putIfAbsent("cmpxCd", cmpxCd);
		Map<String, Object> saved = vocService.createReport(body);
		return Responses.MapResponse.of(saved);
	}

	/**
	 * 대여 카탈로그 (게스트) — Phase D.
	 */
	@ResponseBody
	@RequestMapping(value = "/rental/items", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse listRentalItems() {
		return Responses.ListResponse.of(rentalService.listItems(propCd, cmpxCd));
	}

	/**
	 * 대여 주문 (게스트) — Phase D.
	 */
	@ResponseBody
	@RequestMapping(value = "/rental", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertRental(RequestParams requestParams) {
		Map<String, Object> body = new HashMap<>(requestParams.getParams());
		body.putIfAbsent("propCd", propCd);
		body.putIfAbsent("cmpxCd", cmpxCd);
		Map<String, Object> saved = rentalService.createOrder(body);
		return Responses.MapResponse.of(saved);
	}
}
