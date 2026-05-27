package com.khy.signalingservice.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khy.signalingservice.dto.SignalMessage;
import com.khy.signalingservice.service.SessionRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisSignalSubscriber implements MessageListener {

    private final SessionRegistryService sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            SignalMessage signal = objectMapper.readValue(json, SignalMessage.class);

            // 같은 방의 다른 피어에게 메시지 중계 (발신자 제외)
            for (WebSocketSession session : sessionRegistry.getSessions(signal.getRoomId())) {
                if (session.isOpen() && !session.getId().equals(signal.getSenderId())) {
                    try {
                        session.sendMessage(new TextMessage(signal.getPayload()));
                        log.debug("(중계) {} → {}", signal.getSenderId(), session.getId());
                    } catch (IOException e) {
                        log.error("메시지 전송 실패 - sessionId: {}", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Redis 시그널 메시지 처리 실패", e);
        }
    }
}
