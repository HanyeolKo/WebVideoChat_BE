package com.khy.roomservice.dto.request;

import lombok.Data;

@Data
public class CreateRoomRequest {
    private String title;
    private String password;
    private String content;
}
