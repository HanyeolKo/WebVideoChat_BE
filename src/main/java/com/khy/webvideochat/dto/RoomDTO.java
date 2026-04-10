package com.khy.webvideochat.dto;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Data
public class RoomDTO {
    //채팅방 ID
    String id;

    //채팅방 제목
    String title;

    //채팅방 비밀번호
    String password;

    //채팅방 주제
    String content;

    //접속한 유저(세션으로 구분)
    List<WebSocketSession> chatUsers;

    public RoomDTO(RoomDTO roomInfo){
        this.id = roomInfo.getId();
        this.password = roomInfo.getPassword();
        this.title = roomInfo.getTitle();
        this.content = roomInfo.getContent();
    }
    public RoomDTO(){}
}
