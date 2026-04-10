package com.khy.webvideochat.dto.request;

import lombok.Data;

/**
 * @fileName    EnterRoomRequest.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description 채팅방 입장 요청 DTO. 비밀번호를 URL path가 아닌 body로 전달하여 노출을 방지한다.
 *
 * History:
 * | Date       | Author     | Description |
 * |------------|------------|-------------|
 * | 2026-04-11 | Hanyeol Ko | 최초 작성     |
 */
@Data
public class EnterRoomRequest {
  private String roomId;
  private String password;
}
