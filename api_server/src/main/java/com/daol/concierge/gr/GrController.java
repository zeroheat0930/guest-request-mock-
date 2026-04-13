package com.daol.concierge.gr;

import com.daol.concierge.core.api.ApiResponse;
import com.daol.concierge.core.api.Responses;
import com.daol.concierge.gr.service.GrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 투숙객 요청(Guest Request) 컨트롤러
 */
@Controller
@ResponseBody
@RequestMapping(value = "/api/gr")
public class GrController {

	private static final String APPLICATION_JSON = "application/json;charset=UTF-8";

	@Autowired
	private GrService grService;

	/**
	 * 어메니티 품목 마스터 조회
	 */
	@RequestMapping(value = "/amenity/items", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse getAmenityItemList() {
		return Responses.ListResponse.of(grService.getAmenityItemList());
	}

	/**
	 * 어메니티 요청 목록 조회
	 * @param rsvNo 예약번호 (optional)
	 */
	@RequestMapping(value = "/amenity/list", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public Responses.ListResponse getAmenityList(@RequestParam(required = false) String rsvNo) {
		return Responses.ListResponse.of(grService.getAmenityList(rsvNo));
	}

	/**
	 * 어메니티 요청 등록
	 */
	@RequestMapping(value = "/amenity", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertAmenityReq(@RequestBody Map<String, Object> params) {
		return Responses.MapResponse.of(grService.insertAmenityReq(params));
	}

	/**
	 * 하우스키핑 상태 조회
	 */
	@RequestMapping(value = "/housekeeping", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse getHousekeepingStat(@RequestParam String rsvNo) {
		return Responses.MapResponse.of(grService.getHousekeepingStat(rsvNo));
	}

	/**
	 * 하우스키핑 상태 변경 (MU=객실정비, DND=방해금지, CLR=해제)
	 */
	@RequestMapping(value = "/housekeeping", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse updateHousekeepingStat(@RequestBody Map<String, Object> params) {
		return Responses.MapResponse.of(grService.updateHousekeepingStat(params));
	}

	/**
	 * 레이트 체크아웃 가능여부 및 요금 조회
	 */
	@RequestMapping(value = "/late-checkout", method = RequestMethod.GET, produces = APPLICATION_JSON)
	public ApiResponse getLateCheckoutInfo(@RequestParam String rsvNo, @RequestParam String reqOutTm) {
		return Responses.MapResponse.of(grService.getLateCheckoutInfo(rsvNo, reqOutTm));
	}

	/**
	 * 레이트 체크아웃 신청
	 */
	@RequestMapping(value = "/late-checkout", method = RequestMethod.POST, produces = APPLICATION_JSON)
	public ApiResponse insertLateCheckoutReq(@RequestBody Map<String, Object> params) {
		return Responses.MapResponse.of(grService.insertLateCheckoutReq(params));
	}
}
