package com.khy.webvideochat.service;

import com.khy.webvideochat.dto.RoomDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Getter
public class ChatRoomManageService {

    /**
     * 채팅룸의 정보를 담는 변수
     */
    //다른 곳에서 변경 불가
    private final Map<String, RoomDTO> chatRooms = new HashMap<>();

    @Setter
    private String roomId;

    public String createRoom(RoomDTO roomDTO){
        //id생성
        String roomId = UUID.randomUUID().toString();

        roomDTO.setId(roomId);
        chatRooms.put(roomId, roomDTO);

        return roomId;
    }

    /**
     * 채팅방 정보 삭제
     * @param roomId
     */
    public void destroyChatRoom(String roomId){
        chatRooms.remove(roomId);
    }

    /**
     * 모든 채팅방 정보 가져오기
     * @return  비밀번호 정보 제외
     */
    public List<RoomDTO> getRoomList(){
        ArrayList<RoomDTO> roomInfos = new ArrayList<>(chatRooms.values());
        ArrayList<RoomDTO> tempRoom = new ArrayList<>();
        //비밀번호 탈취 방지를 위해 비밀번호 정보는 초기화
        //비밀번호는 getRoomInfo를통해 가져갈 것
        //깊은복사 처리..
        for(int i = 0; i < roomInfos.size(); i++){

            RoomDTO roomInfo = new RoomDTO(roomInfos.get(i));

            // 비밀번호 초기화
            roomInfo.setPassword("");

            tempRoom.add(roomInfo);
        }

        return tempRoom;
    }

    /**
     * 채팅방 상세정보 가져오기
     * @param roomId
     * @return
     */
    public RoomDTO getRoomInfo(String roomId) {
        return chatRooms.get(roomId);
    }
}
