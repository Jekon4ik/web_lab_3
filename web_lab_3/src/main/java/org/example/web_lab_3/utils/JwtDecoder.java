package org.example.web_lab_3.utils;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;


import org.springframework.web.client.RestTemplate;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class JwtDecoder {
    private final CasdoorProperties openIdConnectProperties;

    public JwtDecoder(CasdoorProperties casdoorProperties) {
        this.openIdConnectProperties = casdoorProperties;
    }

    public Claims decodeToken(String token) {
        try {

            RestTemplate restTemplate = new RestTemplate();
            String jwksJson = restTemplate.getForObject(openIdConnectProperties.getConnectEndpoint() + "/.well-known/jwks",
                    String.class);

            // System.out.println("JWKS JSON: " + jwksJson);

            JWKSet jwkSet = JWKSet.parse(jwksJson);
            List<JWK> keys = jwkSet.getKeys();

            if (keys == null || keys.isEmpty()) {
                throw new RuntimeException("No keys found in JWK Set");
            }

            JWK jwk = keys.get(0);
            RSAPublicKey key = (RSAPublicKey) jwk.toRSAKey().toPublicKey();

            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (Exception e) {
            System.err.println("Failed to decode token:" + e);
            throw new RuntimeException("Failed to decode token", e);
        }
    }
    public Map<String, Object> fetchUserProfile(String userId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = openIdConnectProperties.getApiEndpoint() + "/get-user?id=" + userId;

            // Add token if needed
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Authorization", "Bearer " + openIdConnectProperties.getAdminToken());

            org.springframework.http.HttpEntity<Void> requestEntity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, requestEntity, Map.class);

            return response.getBody();

        } catch (Exception e) {
            System.err.println("Failed to fetch user profile: " + e.getMessage());
            return null;
        }
    }

}