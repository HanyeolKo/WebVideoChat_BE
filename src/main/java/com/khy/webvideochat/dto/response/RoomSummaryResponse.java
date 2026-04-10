package com.khy.webvideochat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @fileName    RoomSummaryResponse.java
 * @author      Hanyeol Ko
 * @date        2026-04-11
 * @description 채팅방 목록 응답 DTO. 비밀번호는 포함하지 않는다.
 *
 * History:
 * | Date       | Author     | Description |
 * |------------|------------|-------------|
 * | 2026-04-11 | Hanyeol Ko | 최초 작성     |
 */
@Data
@AllArgsConstructor
public class RoomSummaryResponse {
  private String id;
  private String title;
  private String content;
}
