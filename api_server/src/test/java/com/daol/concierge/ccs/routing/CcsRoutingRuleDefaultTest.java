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
}
