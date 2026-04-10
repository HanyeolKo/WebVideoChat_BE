package com.khy.webvideochat.websocket;

import com.khy.webvideochat.dto.RoomDTO;
import com.khy.webvideochat.service.ChatRoomManageService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @fileName    SocketHandler.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description WebSocket 시그널링 핸들러. 같은 채팅방 내 피어 간 offer/answer/candidate 메시지를 중계한다.
 *
 * History:
 * | Date       | Author     | Description                              |
 * |------------|------------|------------------------------------------|
 * | 2026-04-11 | Hanyeol Ko | configuration 패키지에서 websocket으로 이동 |
 */
@Component
@Slf4j
public class SocketHandler extends TextWebSocketHandler {

  @Autowired
  ChatRoomManageService chatRoomManageService;

  // 핸드쉐이크 인터셉터가 roomId를 담아놓음 (Thread-Safe)
  @Getter
  private final ThreadLocal<String> roomIdThreadLocal = new ThreadLocal<>();

  /**
   * WebSocket 연결 수립 후 처리 — 해당 채팅방의 세션 목록에 등록
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String roomId = roomIdThreadLocal.get();
    RoomDTO roomInfo = chatRoomManageService.getRoomInfo(roomId);
    try {
      if (roomInfo.getChatUsers() != null) {
        roomInfo.getChatUsers().add(session);
      } else {
        List<WebSocketSession> sessionList = new CopyOnWriteArrayList<>();
        sessionList.add(session);
        roomInfo.setChatUsers(sessionList);
      }
      log.info("{} - 연결됨", session.getId());
    } finally {
      roomIdThreadLocal.remove();
    }
  }

  /**
   * WebSocket 연결 해제 후 처리 — 세션 제거, 방이 비면 채팅방 삭제
   */
  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    String key = getRoomIdBySession(chatRoomManageService.getChatRooms(), session);
    if (key == null) {
      log.info("{} 해당 세션이 포함된 채팅방을 찾지 못함", session.getId());
      return;
    }
    RoomDTO roomInfo = chatRoomManageService.getRoomInfo(key);
    roomInfo.getChatUsers().remove(session);
    if (roomInfo.getChatUsers().isEmpty()) {
      chatRoomManageService.destroyChatRoom(key);
    }
    log.info("{} - 끊어짐", session.getId());
  }

  /**
   * 수신 메시지를 같은 채팅방의 다른 피어에게 중계
   */
  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    log.info("(수신) {} : {}", session.getId(), message.getPayload());
    String roomId = getRoomIdBySession(chatRoomManageService.getChatRooms(), session);
    if (roomId == null) return;

    for (WebSocketSession peer : chatRoomManageService.getRoomInfo(roomId).getChatUsers()) {
      if (peer.isOpen() && !session.getId().equals(peer.getId())) {
        peer.sendMessage(message);
        log.info("(송신) {} : {}", peer.getId(), message.getPayload());
      }
    }
  }

  public String getRoomIdBySession(Map<String, RoomDTO> rooms, WebSocketSession session) {
    return rooms.entrySet().stream()
        .filter(e -> e.getValue().getChatUsers() != null && e.getValue().getChatUsers().contains(session))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }
}
