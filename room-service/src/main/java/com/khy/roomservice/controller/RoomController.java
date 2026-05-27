package com.khy.roomservice.controller;

import com.khy.roomservice.dto.RoomDTO;
import com.khy.roomservice.dto.request.CreateRoomRequest;
import com.khy.roomservice.dto.request.EnterRoomRequest;
import com.khy.roomservice.dto.response.RoomEnterResponse;
import com.khy.roomservice.dto.response.RoomSummaryResponse;
import com.khy.roomservice.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomSummaryResponse>> getRooms() {
        List<RoomSummaryResponse> rooms = roomService.getRoomList().stream()
                .map(room -> new RoomSummaryResponse(room.getId(), room.getTitle(), room.getContent()))
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable String roomId) {
        RoomDTO room = roomService.getRoomInfo(roomId);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @PostMapping
    public ResponseEntity<RoomEnterResponse> createRoom(@RequestBody CreateRoomRequest request) {
        RoomDTO roomDTO = new RoomDTO(null, request.getTitle(), request.getPassword(), request.getContent());
        String roomId = roomService.createRoom(roomDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RoomEnterResponse(roomId, request.getTitle()));
    }

    @PostMapping("/enter")
    public ResponseEntity<RoomEnterResponse> enterRoom(@RequestBody EnterRoomRequest request) {
        RoomDTO roomInfo = roomService.getRoomInfo(request.getRoomId());
        if (roomInfo == null) return ResponseEntity.notFound().build();
        if (!roomInfo.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new RoomEnterResponse(roomInfo.getId(), roomInfo.getTitle()));
    }

    // Signaling Service에서 방이 비었을 때 호출
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}
