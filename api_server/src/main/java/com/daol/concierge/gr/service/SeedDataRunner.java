package com.daol.concierge.gr.service;

import com.daol.concierge.gr.domain.AmenityItem;
import com.daol.concierge.gr.domain.Reservation;
import com.daol.concierge.gr.repo.AmenityItemRepository;
import com.daol.concierge.gr.repo.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초기 시드 데이터 주입 (기동 시 1회)
 *
 * 기존 인메모리 샘플(예약 2건, 어메니티 5품목)을 DB 가 비어있을 때만 INSERT.
 * 재시작 후에도 데이터는 H2 파일(`data/concierge-dev.mv.db`)에 유지됨.
 * 프로덕션에선 별도 시드/마이그레이션 스크립트로 대체 예정.
 */
@Component
public class SeedDataRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
	private static final String DEFAULT_PROP_CD = "HQ";

	@Autowired private ReservationRepository reservationRepo;
	@Autowired private AmenityItemRepository amenityItemRepo;

	@Override
	@Transactional
	public void run(String... args) {
		seedReservations();
		seedAmenityItems();
	}

	private void seedReservations() {
		if (reservationRepo.count() > 0) {
			log.debug("Reservation seed skipped (count={})", reservationRepo.count());
			return;
		}
		// {rsvNo, propCd, roomNo, perNm, chkInDt, chkOutDt, chkOutTm, roomTpCd, perUseLang, birthDt}
		Object[][] rows = {
				{"R2026041300001", "HQ",  "1205", "HONG GILDONG", "20260413", "20260415", "1100", "DLX", "ko_KR", "19800101"},
				{"R2026041300002", "HQ",  "0807", "JOHN SMITH",   "20260413", "20260414", "1100", "STD", "en_US", "19750303"},
				// 제주 프로퍼티 (멀티 프로퍼티 격리 검증용)
				{"R2026041300003", "JJU", "2010", "KIM MINJI",    "20260413", "20260416", "1100", "DLX", "ko_KR", "19900505"}
		};
		for (Object[] row : rows) {
			Reservation r = new Reservation();
			r.setRsvNo((String) row[0]);
			r.setPropCd((String) row[1]);
			r.setRoomNo((String) row[2]);
			r.setPerNm((String) row[3]);
			r.setChkInDt((String) row[4]);
			r.setChkOutDt((String) row[5]);
			r.setChkOutTm((String) row[6]);
			r.setRoomTpCd((String) row[7]);
			r.setPerUseLang((String) row[8]);
			r.setBirthDt((String) row[9]);
			reservationRepo.save(r);
		}
		log.info("Seeded {} reservations", rows.length);
	}

	private void seedAmenityItems() {
		if (amenityItemRepo.count() > 0) {
			log.debug("AmenityItem seed skipped (count={})", amenityItemRepo.count());
			return;
		}
		Object[][] rows = {
				{"AM001", "수건",     "Towel",          4},
				{"AM002", "생수",     "Mineral Water",  6},
				{"AM003", "비누",     "Soap",           3},
				{"AM004", "샴푸",     "Shampoo",        3},
				{"AM005", "칫솔세트", "Toothbrush Set", 4}
		};
		for (Object[] row : rows) {
			AmenityItem it = new AmenityItem();
			it.setItemCd((String) row[0]);
			it.setPropCd(DEFAULT_PROP_CD);
			it.setItemNm((String) row[1]);
			it.setItemNmEng((String) row[2]);
			it.setMaxQty((Integer) row[3]);
			amenityItemRepo.save(it);
		}
		log.info("Seeded {} amenity items", rows.length);
	}
}
