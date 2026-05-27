package com.khy.roomservice.dto.request;

import lombok.Data;

@Data
public class EnterRoomRequest {
    private String roomId;
    private String password;
}
