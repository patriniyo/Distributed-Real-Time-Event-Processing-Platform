package com.drep.dashboard.websocket;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class FakeWebSocketSession implements WebSocketSession {

    private final String id;
    private boolean open = true;
    private final List<TextMessage> sent = new ArrayList<>();

    FakeWebSocketSession(String id) {
        this.id = id;
    }

    List<TextMessage> sentMessages() {
        return sent;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public URI getUri() {
        return URI.create("ws://localhost/ws/events");
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
        sent.add((TextMessage) message);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public void close(CloseStatus status) {
        open = false;
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
        return new HttpHeaders();
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return new ConcurrentHashMap<>();
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public String getAcceptedProtocol() {
        return null;
    }

    @Override
    public void setTextMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getTextMessageSizeLimit() {
        return 0;
    }

    @Override
    public void setBinaryMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getBinaryMessageSizeLimit() {
        return 0;
    }
}
