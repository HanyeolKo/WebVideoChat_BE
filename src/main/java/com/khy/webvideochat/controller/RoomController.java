package com.khy.webvideochat.controller;

import com.khy.webvideochat.dto.RoomDTO;
import com.khy.webvideochat.dto.request.CreateRoomRequest;
import com.khy.webvideochat.dto.request.EnterRoomRequest;
import com.khy.webvideochat.dto.response.RoomEnterResponse;
import com.khy.webvideochat.dto.response.RoomSummaryResponse;
import com.khy.webvideochat.service.ChatRoomManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @fileName    RoomController.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description 채팅방 REST API 컨트롤러.
 *              기존 Thymeleaf 기반의 JoinController + ChatRoomController를 REST로 통합.
 *
 * History:
 * | Date       | Author     | Description                            |
 * |------------|------------|----------------------------------------|
 * | 2026-04-11 | Hanyeol Ko | JoinController, ChatRoomController 통합 |
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final ChatRoomManageService chatRoomManageService;

  /**
   * 채팅방 목록 조회 (비밀번호 제외)
   */
  @GetMapping
  public ResponseEntity<List<RoomSummaryResponse>> getRooms() {
    List<RoomSummaryResponse> rooms = chatRoomManageService.getRoomList().stream()
        .map(room -> new RoomSummaryResponse(room.getId(), room.getTitle(), room.getContent()))
        .toList();
    return ResponseEntity.ok(rooms);
  }

  /**
   * 채팅방 생성
   */
  @PostMapping
  public ResponseEntity<RoomEnterResponse> createRoom(@RequestBody CreateRoomRequest request) {
    RoomDTO roomDTO = new RoomDTO();
    roomDTO.setTitle(request.getTitle());
    roomDTO.setPassword(request.getPassword());
    roomDTO.setContent(request.getContent());

    String roomId = chatRoomManageService.createRoom(roomDTO);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new RoomEnterResponse(roomId, request.getTitle()));
  }

  /**
   * 채팅방 입장 — 비밀번호 검증 후 roomId/roomName 반환
   * 비밀번호는 URL 노출 방지를 위해 request body로 수신
   */
  @PostMapping("/enter")
  public ResponseEntity<RoomEnterResponse> enterRoom(@RequestBody EnterRoomRequest request) {
    RoomDTO roomInfo = chatRoomManageService.getRoomInfo(request.getRoomId());
    if (roomInfo == null) {
      return ResponseEntity.notFound().build();
    }
    if (!roomInfo.getPassword().equals(request.getPassword())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(new RoomEnterResponse(roomInfo.getId(), roomInfo.getTitle()));
  }
}
