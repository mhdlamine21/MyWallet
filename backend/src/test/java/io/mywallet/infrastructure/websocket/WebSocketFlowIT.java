package io.mywallet.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.auth.interfaces.rest.dto.RegisterRequest;
import io.mywallet.marketdata.application.MarketDataGeneratorService;
import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end over a real embedded server (not MockMvc, which can't do a WebSocket
 * upgrade) - this is what actually proves the JWT handshake gate and the live price
 * broadcast work together, not just that each compiles in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketFlowIT extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AssetJpaRepository assetRepository;
    @Autowired private MarketDataGeneratorService marketDataGeneratorService;

    @Test
    void authenticatedClientReceivesLivePriceTicksOverWebSocket() throws Exception {
        String token = registerAndGetAccessToken("wsuser+" + System.nanoTime() + "@mywallet.dev");

        AssetEntity asset = assetRepository.save(new AssetEntity(
            UUID.randomUUID(), "WS" + System.nanoTime(), AssetEntity.AssetClass.CRYPTO, "USD",
            "WebSocket test asset", new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("0.3"), 1L
        ));

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CompletableFuture<WebSocketBroadcaster.PriceTick> received = new CompletableFuture<>();

        StompSession session = stompClient
            .connectAsync("ws://localhost:" + port + "/ws?token=" + token, new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/prices/" + asset.getSymbol(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return WebSocketBroadcaster.PriceTick.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((WebSocketBroadcaster.PriceTick) payload);
            }
        });

        Thread.sleep(300); // let the subscription register server-side before the tick fires
        marketDataGeneratorService.tick(); // trigger a tick manually - the real @Scheduled interval is too slow for a test

        WebSocketBroadcaster.PriceTick tick = received.get(5, TimeUnit.SECONDS);
        assertThat(tick.symbol()).isEqualTo(asset.getSymbol());
        assertThat(tick.price()).isNotNull();

        session.disconnect();
    }

    @Test
    void handshakeWithoutATokenIsRejected() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        assertThatThrownBy(() ->
            stompClient.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS)
        ).isInstanceOf(ExecutionException.class);
    }

    @Test
    void handshakeWithAnInvalidTokenIsRejected() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        assertThatThrownBy(() ->
            stompClient.connectAsync("ws://localhost:" + port + "/ws?token=not-a-real-jwt", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS)
        ).isInstanceOf(ExecutionException.class);
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        RequestEntity<RegisterRequest> request = RequestEntity
            .post(URI.create("/api/auth/register"))
            .contentType(MediaType.APPLICATION_JSON)
            .body(new RegisterRequest(email, "a-strong-password-123"));
        String body = restTemplate.exchange(request, String.class).getBody();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
