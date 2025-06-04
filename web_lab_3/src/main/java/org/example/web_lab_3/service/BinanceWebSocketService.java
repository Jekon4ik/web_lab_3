package org.example.web_lab_3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.web_lab_3.webSockets.MarketDataWebSocketHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.example.web_lab_3.protobuf.Ticker;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@Service
public class BinanceWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketService.class);
    private final MarketDataWebSocketHandler webSocketHandler;
    private WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> symbols = Arrays.asList("ethusdt", "bnbusdt", "solusdt");

    public BinanceWebSocketService(MarketDataWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @PostConstruct
    public void connect() {
        try {
            String streamNames = String.join("/", symbols.stream().map(s -> s + "@trade").toArray(String[]::new));
            URI uri = new URI("wss://stream.binance.com:9443/stream?streams=" + streamNames);
            logger.info("Connecting to Binance WebSocket: {}", uri);

            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    logger.info("Binance WebSocket connection established");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JsonNode root = objectMapper.readTree(message);
                        JsonNode data = root.get("data");
                        if (data != null) {
                            String symbol = data.get("s").asText();
                            String price = data.get("p").asText();
                            long eventTime = data.get("E").asLong();

                            Ticker.TickerData proto = Ticker.TickerData.newBuilder()
                                    .setSymbol(symbol)
                                    .setPrice(price)
                                    .setEventTime(eventTime)
                                    .build();

                            webSocketHandler.broadcastBinaryMessage(proto.toByteArray());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to process Binance message", e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.warn("Binance WebSocket closed. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
                }

                @Override
                public void onError(Exception ex) {
                    logger.error("Binance WebSocket error", ex);
                }
            };
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            logger.error("Invalid Binance WebSocket URI", e);
        } catch (Exception e) {
            logger.error("Failed to connect to Binance WebSocket", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
            logger.info("Disconnected from Binance WebSocket (on shutdown)");
        }
    }
}