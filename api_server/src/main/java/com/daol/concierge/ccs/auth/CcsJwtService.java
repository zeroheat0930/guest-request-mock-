package com.daol.concierge.ccs.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class CcsJwtService {

	@Value("${ccs.jwt.secret:${jwt.secret}}")
	private String secret;

	@Value("${ccs.jwt.hours-valid:12}")
	private long hoursValid;

	private SecretKey key() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String issue(String staffId, String staffNm, String propCd, String cmpxCd, String deptCd, String userTp) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(hoursValid * 3600);

		return Jwts.builder()
				.subject(staffId)
				.claim("type", "STAFF")
				.claim("staffId", staffId)
				.claim("loginId", staffId)
				.claim("deptCd", deptCd)
				.claim("propCd", propCd)
				.claim("cmpxCd", cmpxCd)
				.claim("staffNm", staffNm)
				.claim("userTp", userTp)
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(key())
				.compact();
	}

	public CcsPrincipal parse(String token) {
		try {
			Claims c = Jwts.parser()
					.verifyWith(key())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			if (!"STAFF".equals(c.get("type", String.class))) return null;
			return new CcsPrincipal(
					c.get("staffId", String.class),
					c.get("loginId", String.class),
					c.get("deptCd", String.class),
					c.get("propCd", String.class),
					c.get("cmpxCd", String.class),
					c.get("staffNm", String.class),
					c.get("userTp", String.class)
			);
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
