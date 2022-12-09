package com.example.finalproject.jwt;

import com.example.finalproject.controller.request.TokenDto;
import com.example.finalproject.controller.response.ResponseDto;
import com.example.finalproject.domain.Member;
import com.example.finalproject.domain.RefreshToken;
import com.example.finalproject.domain.UserDetailsImpl;
import com.example.finalproject.exception.PrivateResponseBody;
import com.example.finalproject.exception.StatusCode;
import com.example.finalproject.repository.RefreshTokenRepository;
import com.example.finalproject.shared.Authority;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class TokenProvider {

  private static final String AUTHORITIES_KEY = "auth";
  private static final String BEARER_PREFIX = "Bearer ";
//  private static final long ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000L; // 30분
//  private static final long REFRESH_TOKEN_EXPRIRE_TIME = 30 * 60 * 1000L; // 30분
  private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7;       //7일
  private static final long REFRESH_TOKEN_EXPRIRE_TIME = 1000 * 60 * 60 * 24 * 7;     //7일

  private final Key key;

  private final RefreshTokenRepository refreshTokenRepository;

  public TokenProvider(@Value("${jwt.secret}") String secretKey,
      RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  public TokenDto generateTokenDto(Member member) {
    long now = (new Date().getTime());

    Date accessTokenExpiresIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
    String accessToken = Jwts.builder()
        .setSubject(member.getEmail())
        .claim(AUTHORITIES_KEY, Authority.ROLE_MEMBER.toString())
        .setExpiration(accessTokenExpiresIn)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();

    String refreshToken = Jwts.builder()
        .setExpiration(new Date(now + REFRESH_TOKEN_EXPRIRE_TIME))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();

    RefreshToken refreshTokenObject = RefreshToken.builder()
        .id(member.getMemberId())
        .member(member)
        .value(refreshToken)
        .build();

    refreshTokenRepository.save(refreshTokenObject);

    return TokenDto.builder()
        .grantType(BEARER_PREFIX)
        .accessToken(accessToken)
        .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
        .refreshToken(refreshToken)
        .build();

  }

  public Member getMemberFromAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || AnonymousAuthenticationToken.class.
        isAssignableFrom(authentication.getClass())) {
      return null;
    }
    return ((UserDetailsImpl) authentication.getPrincipal()).getMember();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.info("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
    } catch (ExpiredJwtException e) {
      log.info("Expired JWT token, 만료된 JWT token 입니다.");
    } catch (UnsupportedJwtException e) {
      log.info("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
    } catch (IllegalArgumentException e) {
      log.info("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
    }
    return false;
  }

  @Transactional(readOnly = true)
  public RefreshToken isPresentRefreshToken(Member member) {
    Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findByMember(member);
    return optionalRefreshToken.orElse(null);
  }

  @Transactional
  public ResponseEntity<?> deleteRefreshToken(Member member) {

    RefreshToken refreshToken = isPresentRefreshToken(member);

    if (null == refreshToken) {
      return new ResponseEntity<>(new PrivateResponseBody
              (StatusCode.LOGIN_WRONG_FORM_JWT_TOKEN,null), HttpStatus.NOT_FOUND);
    }

    refreshTokenRepository.delete(refreshToken);

    return new ResponseEntity<>(new PrivateResponseBody
            (StatusCode.OK,"로그아웃"),HttpStatus.OK);
  }
}
