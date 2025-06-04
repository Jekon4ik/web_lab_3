package org.example.web_lab_3.webSockets;

import io.jsonwebtoken.Claims;
import org.example.web_lab_3.utils.CasdoorProperties;
import org.example.web_lab_3.utils.JwtDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final JwtDecoder jwtDecoder;

    public MarketDataWebSocketHandler(CasdoorProperties casdoorProperties) {
        this.jwtDecoder = new JwtDecoder(casdoorProperties);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String accessToken = null;
        // Extract access_token from cookies
        Map<String, Object> attributes = session.getAttributes();
        if (session.getHandshakeHeaders().containsKey("cookie")) {
            for (String cookieHeader : session.getHandshakeHeaders().get("cookie")) {
                String[] cookies = cookieHeader.split(";");
                for (String cookie : cookies) {
                    String[] parts = cookie.trim().split("=", 2);
                    if (parts.length == 2 && parts[0].equals("access_token")) {
                        accessToken = parts[1];
                        break;
                    }
                }
            }
        }

        if (accessToken == null) {
            logger.warn("WebSocket connection rejected: no access_token cookie");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing access_token"));
            return;
        }

        try {
            Claims claims = jwtDecoder.decodeToken(accessToken);
            logger.info("WebSocket authenticated for user: {}", claims.getSubject());
            sessions.put(session.getId(), session);
        } catch (Exception e) {
            logger.warn("WebSocket connection rejected: invalid token");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid or expired token"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        logger.debug("Text message received from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        logger.debug("Binary message received from {} ({} bytes)", session.getId(), message.getPayloadLength());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("Session {} closed. Code: {}, Reason: {}", session.getId(), status.getCode(), status.getReason());
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket error in session {}: {}", session.getId(), exception.getMessage());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            logger.error("Failed to close session {} after error", session.getId());
        }
        sessions.remove(session.getId());
    }

    public void broadcastBinaryMessage(byte[] binaryData) {
        BinaryMessage message = new BinaryMessage(binaryData);
        sessions.forEach((id, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                logger.error("Failed to send binary message to session {}", id);
            }
        });
    }

    public void broadcastTextMessage(String text) {
        TextMessage message = new TextMessage(text);
        sessions.forEach((id, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                logger.error("Failed to send text message to session {}", id);
            }
        });
    }
}