package com.khy.roomservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khy.roomservice.dto.RoomDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String ROOMS_SET_KEY = "rooms";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public String createRoom(RoomDTO roomDTO) {
        String roomId = UUID.randomUUID().toString();
        roomDTO.setId(roomId);
        saveRoom(roomDTO);
        redisTemplate.opsForSet().add(ROOMS_SET_KEY, roomId);
        log.info("채팅방 생성: {}", roomId);
        return roomId;
    }

    public void deleteRoom(String roomId) {
        redisTemplate.delete(ROOM_KEY_PREFIX + roomId);
        redisTemplate.opsForSet().remove(ROOMS_SET_KEY, roomId);
        log.info("채팅방 삭제: {}", roomId);
    }

    public List<RoomDTO> getRoomList() {
        Set<String> roomIds = redisTemplate.opsForSet().members(ROOMS_SET_KEY);
        List<RoomDTO> rooms = new ArrayList<>();
        if (roomIds == null) return rooms;

        for (String roomId : roomIds) {
            RoomDTO room = findRoom(roomId);
            if (room != null) {
                room.setPassword("");
                rooms.add(room);
            }
        }
        return rooms;
    }

    public RoomDTO getRoomInfo(String roomId) {
        return findRoom(roomId);
    }

    private void saveRoom(RoomDTO room) {
        try {
            String json = objectMapper.writeValueAsString(room);
            redisTemplate.opsForValue().set(ROOM_KEY_PREFIX + room.getId(), json);
        } catch (JsonProcessingException e) {
            log.error("채팅방 직렬화 실패: {}", room.getId(), e);
            throw new RuntimeException("채팅방 저장 실패", e);
        }
    }

    private RoomDTO findRoom(String roomId) {
        String json = redisTemplate.opsForValue().get(ROOM_KEY_PREFIX + roomId);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, RoomDTO.class);
        } catch (JsonProcessingException e) {
            log.error("채팅방 역직렬화 실패: {}", roomId, e);
            return null;
        }
    }
}
