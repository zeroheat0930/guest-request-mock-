package com.daol.concierge.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 게스트 토큰 발급/검증 서비스
 *
 * HS256 대칭키 서명. 만료 시간은 `jwt.hours-valid` 와 예약 chkOutDt 23:59 중 먼저 오는 시각.
 */
@Service
public class JwtService {

	private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.hours-valid:72}")
	private long hoursValid;

	private SecretKey key() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String issue(String rsvNo, String propCd, String chkOutDt) {
		Instant now = Instant.now();
		Instant hardCap = now.plusSeconds(hoursValid * 3600);
		Instant resvExpiry = LocalDate.parse(chkOutDt, FMT_DT)
				.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
		Instant exp = hardCap.isBefore(resvExpiry) ? hardCap : resvExpiry;

		return Jwts.builder()
				.subject(rsvNo)
				.claim("propCd", propCd)
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(key())
				.compact();
	}

	/**
	 * @return 파싱된 claims, 실패 시 null
	 */
	public Claims parse(String token) {
		try {
			return Jwts.parser()
					.verifyWith(key())
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
