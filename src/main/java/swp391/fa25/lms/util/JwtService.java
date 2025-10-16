package swp391.fa25.lms.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    // Chu ky duoc ma hoa bang HS256
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long accessTokenValidity = 15 * 60 * 1000; // 15 phút
    private final long refreshTokenValidity = 7 * 24 * 60 * 60 * 1000; // 7 ngày

    public String generateAccessToken(Account account) {
        return Jwts.builder()
                .setSubject(account.getEmail())
                .claim("role", account.getRole().getRoleName().name())
                .claim("accountId", account.getAccountId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Account account) {
        return Jwts.builder()
                .setSubject(account.getEmail())
                .claim("accountId", account.getAccountId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}