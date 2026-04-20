package com.daol.concierge.auth;

public record GuestPrincipal(String rsvNo, String propCd, String cmpxCd) {
	@Override
	public String toString() {
		return rsvNo + "@" + propCd + "/" + cmpxCd;
	}
}
