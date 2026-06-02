package com.drep.dashboard.config;

import com.drep.dashboard.websocket.LiveEventWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveEventWebSocketHandler liveEventWebSocketHandler;

    public WebSocketConfig(LiveEventWebSocketHandler liveEventWebSocketHandler) {
        this.liveEventWebSocketHandler = liveEventWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveEventWebSocketHandler, "/ws/events")
                .setAllowedOrigins("*");
    }
}
