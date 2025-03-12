package com.example.TripSpring.security;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.repository.UserRepository;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    private UserRepository userRepository ;
    private String secretKey;
    private long accessTokenValidityInSeconds;
    private long refreshTokenValidityInSeconds;
    private final CustomUserDetailsService userDetailsService;

    public JwtTokenProvider(
        String jwtSecret, 
        long accessTokenValidity, 
        long refreshTokenValidity, 
        CustomUserDetailsService userDetailsService
    ) {
        this.secretKey = jwtSecret;
        this.accessTokenValidityInSeconds = accessTokenValidity;
        this.refreshTokenValidityInSeconds = refreshTokenValidity;
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.getTokenValidityInMilliseconds());

        return Jwts.builder()
            .setSubject(authentication.getName())
            .claim("auth", authorities)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .setExpiration(validity)
            .compact();
    }

    public String createToken(String username) {
        Claims claims = Jwts.claims().setSubject(username);
        Date now = new Date();
        Date validity = new Date(now.getTime() + this.getTokenValidityInMilliseconds());

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }

    public String createRefreshToken(String username) {
        Claims claims = Jwts.claims().setSubject(username);
        Date now = new Date();
        Date validity = new Date(now.getTime() + this.getRefreshTokenValidityInMilliseconds());

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }

    // 기존 메서드들 그대로 유지
    public Authentication getAuthentication(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();

            log.info("Token Claims: {}", claims);
            String email = claims.getSubject();
            log.info("Email from token: {}", email);

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            log.info("UserDetails: {}", userDetails);

            return new UsernamePasswordAuthenticationToken(
                userDetails, 
                token, 
                userDetails.getAuthorities()
            );
        } catch (Exception e) {
            log.error("Authentication Error", e);
            throw new RuntimeException("Invalid token", e);
        }
    }
    public String getUsername(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
    }

    public long getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();
            
        return claims.getExpiration().getTime() - new Date().getTime();
    }

    public boolean validateToken(String token) {
        try {
            log.info("Validating token: {}", token.substring(0, Math.min(10, token.length())) + "...");
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            log.info("Token is valid");
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public long getTokenValidityInMilliseconds() {
        return accessTokenValidityInSeconds * 1000;
    }

    public long getRefreshTokenValidityInMilliseconds() {
        return refreshTokenValidityInSeconds * 1000;
    }

    public String createSocialLoginToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        
        // 소셜 로그인 사용자를 위한 기본 권한 추가
        claims.put("auth", "ROLE_USER");
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + getTokenValidityInMilliseconds());
    
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .compact();
    }
}