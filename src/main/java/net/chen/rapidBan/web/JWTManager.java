package net.chen.rapidBan.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import net.chen.rapidBan.RapidBan;

import java.util.Date;

public class JWTManager {
    private final Algorithm algorithm;
    private final long expirationTime = 24 * 60 * 60 * 1000; // 24 hours

    public JWTManager(String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(String username, String role) {
        return JWT.create()
            .withSubject(username)
            .withClaim("role", role)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
            .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return JWT.require(algorithm).build().verify(token);
    }

    public String extractUsername(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getSubject();
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return jwt.getClaim("role").asString();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
