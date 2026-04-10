package com.khy.webvideochat.dto.request;

import lombok.Data;

/**
 * @fileName    CreateRoomRequest.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description 채팅방 생성 요청 DTO
 *
 * History:
 * | Date       | Author     | Description |
 * |------------|------------|-------------|
 * | 2026-04-11 | Hanyeol Ko | 최초 작성     |
 */
@Data
public class CreateRoomRequest {
  private String title;
  private String password;
  private String content;
}
