package com.khy.roomservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomEnterResponse {
    private String roomId;
    private String roomName;
}
