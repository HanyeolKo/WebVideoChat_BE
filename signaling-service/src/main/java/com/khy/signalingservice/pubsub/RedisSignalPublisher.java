package com.khy.signalingservice.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khy.signalingservice.dto.SignalMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisSignalPublisher {

    private static final String SIGNAL_CHANNEL_PREFIX = "signal:room:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(SignalMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(SIGNAL_CHANNEL_PREFIX + message.getRoomId(), json);
        } catch (JsonProcessingException e) {
            log.error("시그널 메시지 직렬화 실패", e);
        }
    }
}
