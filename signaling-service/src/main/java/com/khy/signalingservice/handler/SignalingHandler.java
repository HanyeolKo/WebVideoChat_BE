package com.khy.signalingservice.handler;

import com.khy.signalingservice.dto.SignalMessage;
import com.khy.signalingservice.pubsub.RedisSignalPublisher;
import com.khy.signalingservice.service.RoomServiceClient;
import com.khy.signalingservice.service.SessionRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class SignalingHandler extends TextWebSocketHandler {

    private static final String USER_COUNT_KEY_PREFIX = "room:";
    private static final String USER_COUNT_KEY_SUFFIX = ":users";

    private final SessionRegistryService sessionRegistry;
    private final RedisSignalPublisher publisher;
    private final RoomServiceClient roomServiceClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomId(session);

        if (!roomServiceClient.roomExists(roomId)) {
            log.warn("존재하지 않는 채팅방 접속 시도: {}", roomId);
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        sessionRegistry.addSession(roomId, session);
        redisTemplate.opsForValue().increment(USER_COUNT_KEY_PREFIX + roomId + USER_COUNT_KEY_SUFFIX);
        log.info("세션 연결됨 - sessionId: {}, roomId: {}", session.getId(), roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String roomId = sessionRegistry.getRoomIdBySession(session);
        if (roomId == null) return;

        log.debug("(수신) sessionId: {}, roomId: {}", session.getId(), roomId);
        publisher.publish(new SignalMessage(roomId, session.getId(), message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = sessionRegistry.removeSession(session);
        if (roomId == null) return;

        Long remaining = redisTemplate.opsForValue()
                .decrement(USER_COUNT_KEY_PREFIX + roomId + USER_COUNT_KEY_SUFFIX);

        if (remaining != null && remaining <= 0) {
            redisTemplate.delete(USER_COUNT_KEY_PREFIX + roomId + USER_COUNT_KEY_SUFFIX);
            roomServiceClient.deleteRoom(roomId);
        }

        log.info("세션 종료됨 - sessionId: {}, roomId: {}", session.getId(), roomId);
    }

    private String getRoomId(WebSocketSession session) {
        return (String) session.getAttributes().get("roomId");
    }
}
