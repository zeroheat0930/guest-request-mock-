package com.daol.concierge.ccs.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CcsRoutingRuleDefaultTest {

    private CcsRoutingRuleDefault rule;

    @BeforeEach
    void setUp() {
        rule = new CcsRoutingRuleDefault();
    }

    @Test
    void amenityRoutesToHk() {
        assertEquals("HK", rule.deptCdFor("AMENITY"));
    }

    @Test
    void hkMuRoutesToHkViaPrefixMatch() {
        assertEquals("HK", rule.deptCdFor("HK_MU"));
    }

    @Test
    void lateCoRoutesToFr() {
        assertEquals("FR", rule.deptCdFor("LATE_CO"));
    }

    @Test
    void parkingRoutesToEng() {
        assertEquals("ENG", rule.deptCdFor("PARKING"));
    }

    @Test
    void chatReturnsNull() {
        assertNull(rule.deptCdFor("CHAT"));
    }

    @Test
    void nearbyReturnsNull() {
        assertNull(rule.deptCdFor("NEARBY"));
    }

    @Test
    void unknownTypeReturnsNull() {
        assertNull(rule.deptCdFor("UNKNOWN_TYPE"));
    }

    @Test
    void lostFoundRoutesToFr() {
        assertEquals("FR", rule.deptCdFor("LOSTFOUND"));
    }

    @Test
    void vocRoutesToFr() {
        assertEquals("FR", rule.deptCdFor("VOC"));
    }

    @Test
    void rentalRoutesToFr() {
        assertEquals("FR", rule.deptCdFor("RENTAL"));
    }

    @Test
    void dutyReturnsNull() {
        // DUTY 는 부서 라우팅 없음 — 관리자 전용 도메인
        assertNull(rule.deptCdFor("DUTY"));
    }
}
