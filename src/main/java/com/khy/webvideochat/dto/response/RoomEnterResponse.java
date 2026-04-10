package com.khy.webvideochat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @fileName    RoomEnterResponse.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description 채팅방 생성/입장 성공 응답 DTO
 *
 * History:
 * | Date       | Author     | Description |
 * |------------|------------|-------------|
 * | 2026-04-11 | Hanyeol Ko | 최초 작성     |
 */
@Data
@AllArgsConstructor
public class RoomEnterResponse {
  private String roomId;
  private String roomName;
}
