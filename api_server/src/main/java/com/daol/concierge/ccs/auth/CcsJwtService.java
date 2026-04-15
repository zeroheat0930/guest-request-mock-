package com.daol.concierge.ccs.auth;

import com.daol.concierge.ccs.domain.CcsStaff;
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

/**
 * CCS 스태프 토큰 발급/검증 서비스.
 *
 * 게스트 토큰과 같은 비밀키를 공유하되 type=STAFF claim 으로 분리.
 * type 이 STAFF 가 아니면 parse() 가 실패한다.
 */
@Service
public class CcsJwtService {

	@Value("${ccs.jwt.secret:${jwt.secret}}")
	private String secret;

	@Value("${ccs.jwt.hours-valid:12}")
	private long hoursValid;

	private SecretKey key() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String issue(CcsStaff staff) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(hoursValid * 3600);

		return Jwts.builder()
				.subject(staff.getStaffId())
				.claim("type", "STAFF")
				.claim("staffId", staff.getStaffId())
				.claim("loginId", staff.getLoginId())
				.claim("deptCd", staff.getDeptCd())
				.claim("propCd", staff.getPropCd())
				.claim("cmpxCd", staff.getCmpxCd())
				.claim("staffNm", staff.getStaffNm())
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(key())
				.compact();
	}

	/**
	 * @return 파싱된 principal, 실패/타입불일치 시 null
	 */
	public CcsPrincipal parse(String token) {
		try {
			Claims c = Jwts.parser()
					.verifyWith(key())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			String type = c.get("type", String.class);
			if (!"STAFF".equals(type)) {
				return null;
			}
			return new CcsPrincipal(
					c.get("staffId", String.class),
					c.get("loginId", String.class),
					c.get("deptCd", String.class),
					c.get("propCd", String.class),
					c.get("cmpxCd", String.class),
					c.get("staffNm", String.class)
			);
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
