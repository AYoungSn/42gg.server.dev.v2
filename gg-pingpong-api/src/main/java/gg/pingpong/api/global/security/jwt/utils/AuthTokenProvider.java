package gg.pingpong.api.global.security.jwt.utils;

import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Component;

import gg.pingpong.api.global.security.config.properties.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthTokenProvider {

	private AppProperties appProperties;
	private final Key key;
	private final Key refreshKey;

	public AuthTokenProvider(AppProperties appProperties) {
		this.appProperties = appProperties;
		key = Keys.hmacShaKeyFor(appProperties.getAuth().getTokenSecret().getBytes());
		refreshKey = Keys.hmacShaKeyFor(appProperties.getAuth().getRefreshTokenSecret().getBytes());
		log.info(key.getAlgorithm());
	}

	public String refreshToken(Long userId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime()
			+ appProperties.getAuth().getRefreshTokenExpiry());
		return Jwts.builder()
			.setSubject(userId.toString())
			.setIssuedAt(new Date())
			.setExpiration(expiryDate)
			.signWith(refreshKey)
			.compact();
	}

	public String createToken(Long userId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime()
			+ appProperties.getAuth().getTokenExpiry());
		return Jwts.builder()
			.setSubject(Long.toString(userId))
			.setIssuedAt(new Date())
			.setExpiration(expiryDate)
			.signWith(key)
			.compact();
	}

	private Claims getClaims(String token, Key key) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();
		} catch (SecurityException e) {
			log.info("Invalid JWT signature.");
		} catch (MalformedJwtException e) {
			log.info("Invalid JWT token.");
		} catch (ExpiredJwtException e) {
			log.info("Expired JWT token.");
		} catch (UnsupportedJwtException e) {
			log.info("Unsupported JWT token.");
		} catch (IllegalArgumentException e) {
			log.info("JWT token compact of handler are invalid.");
		}
		return null;
	}

	public Long getUserIdFromAccessToken(String accessToken) {
		Claims claims = getClaims(accessToken, key);
		if (claims == null) {
			return null;
		}
		return Long.valueOf(claims.getSubject());
	}

}
