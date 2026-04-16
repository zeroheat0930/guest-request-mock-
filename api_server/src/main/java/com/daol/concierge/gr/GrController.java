package com.daol.concierge.gr;

import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.core.controller.BaseController;
import com.daol.concierge.core.parameter.RequestParams;
import com.daol.concierge.gr.service.GrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 투숙객 요청(Guest Request) 컨트롤러
 */
@Controller
@RequestMapping(value = "/api/gr")
public class GrController extends BaseController {

	@Autowired
	private GrService grService;

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
}
