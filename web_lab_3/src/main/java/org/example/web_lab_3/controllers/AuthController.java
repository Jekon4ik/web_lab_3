package org.example.web_lab_3.controllers;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.example.web_lab_3.utils.CasdoorProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/authentication")
public class AuthController {

    private final CasdoorProperties openIdConnectProperties;
    private final String redirectUri = "https://localhost:8088/authentication/callback";

    public AuthController(CasdoorProperties openIdConnectProperties) {
        this.openIdConnectProperties = openIdConnectProperties;
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        System.out.println("Login requested");

        String authorizeUrl = openIdConnectProperties.getConnectEndpoint() + "/login/oauth/authorize" +
                "?client_id=" + openIdConnectProperties.getConnectClientId() +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&scope=openid profile email";

        System.out.println("Authorize URL: " + authorizeUrl);

        response.sendRedirect(authorizeUrl);
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
        System.out.println("Code: " + code);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", openIdConnectProperties.getConnectClientId());
        params.add("client_secret", openIdConnectProperties.getConnectClientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        System.out.println("Request to get token: " + request);

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                openIdConnectProperties.getConnectEndpoint() + "/api/login/oauth/access_token",
                request,
                Map.class);

        // Handle the token response
        if (tokenResponse.getStatusCode().is2xxSuccessful()) {

            String accessToken = (String) tokenResponse.getBody().get("access_token");

            if (accessToken != null) {
                Cookie accessTokenCookie = new Cookie("access_token", accessToken);
                accessTokenCookie.setPath("/");
                accessTokenCookie.setSecure(true);
                response.addCookie(accessTokenCookie);
            } else {
                System.err.println("Failed to get access token from response.");
            }
        } else {
            System.err.println("Failed to get token: " + tokenResponse.getStatusCode());
        }

        response.sendRedirect("/");
    }

}
