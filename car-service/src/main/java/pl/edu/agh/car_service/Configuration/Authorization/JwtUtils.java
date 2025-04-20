package pl.edu.agh.car_service.Configuration.Authorization;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    @Value("${car_service.app.jwtSecret}")
    private String jwtSecret;

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Token parsing
    public Claims extractClaims(String token) {
        if (token.contains("Bearer"))
                token = token.replace("Bearer", "");

        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUserMail(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (JwtException ex) {
            return null;
        }
    }

    public Long getUserId(String token) {
        try {
            return extractClaims(token).get("userId", Long.class);
        } catch (JwtException ex) {
            return null;
        }
    }

    public String getRole(String token) {
        try {
            return extractClaims(token).get("role", String.class);
        } catch (JwtException ex) {
            return null;
        }
    }
}
