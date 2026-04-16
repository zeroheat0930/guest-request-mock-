package com.daol.concierge.dev;

import com.daol.concierge.pms.mapper.PmsMapper;
import com.daol.concierge.inv.mapper.InvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dev")
@Profile("dev")
public class DevQueryController {

	@Autowired private PmsMapper pmsMapper;
	@Autowired private InvMapper invMapper;

	@GetMapping("/reservations")
	public List<Map<String, Object>> reservations(
			@RequestParam(defaultValue = "0000000001") String propCd,
			@RequestParam(defaultValue = "00001") String cmpxCd) {
		return pmsMapper.selectReservationList(propCd, cmpxCd);
	}

	@GetMapping("/reservation")
	public Map<String, Object> reservation(
			@RequestParam(defaultValue = "0000000001") String propCd,
			@RequestParam(defaultValue = "00001") String cmpxCd,
			@RequestParam String resvNo) {
		Map<String, Object> r = pmsMapper.selectReservation(propCd, cmpxCd, resvNo);
		return r != null ? r : Map.of("error", "not found");
	}

	@GetMapping("/features")
	public List<Map<String, Object>> features(
			@RequestParam(defaultValue = "0000000001") String propCd,
			@RequestParam(defaultValue = "00001") String cmpxCd) {
		return invMapper.selectFeatures(propCd, cmpxCd);
	}

	@GetMapping("/amenity-items")
	public List<Map<String, Object>> amenityItems(
			@RequestParam(defaultValue = "0000000001") String propCd,
			@RequestParam(defaultValue = "00001") String cmpxCd) {
		return invMapper.selectAmenityItems(propCd, cmpxCd);
	}

	@GetMapping("/users")
	public List<Map<String, Object>> users(
			@RequestParam(defaultValue = "0000000001") String propCd,
			@RequestParam(defaultValue = "00001") String cmpxCd,
			@RequestParam(required = false) String deptCd) {
		return pmsMapper.selectUsersByDept(propCd, cmpxCd, deptCd);
	}
}
