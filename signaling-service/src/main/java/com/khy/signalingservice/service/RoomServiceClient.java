package com.khy.signalingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceClient {

    private final RestTemplate restTemplate;

    @Value("${room-service.url:http://ROOM-SERVICE}")
    private String roomServiceUrl;

    public boolean roomExists(String roomId) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    roomServiceUrl + "/api/rooms/" + roomId, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.warn("채팅방 존재 확인 실패 - roomId: {}, 오류: {}", roomId, e.getMessage());
            return false;
        }
    }

    public void deleteRoom(String roomId) {
        try {
            restTemplate.delete(roomServiceUrl + "/api/rooms/" + roomId);
            log.info("채팅방 삭제 요청 완료: {}", roomId);
        } catch (Exception e) {
            log.warn("채팅방 삭제 요청 실패 - roomId: {}, 오류: {}", roomId, e.getMessage());
        }
    }
}
