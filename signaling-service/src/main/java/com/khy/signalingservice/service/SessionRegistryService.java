package com.khy.signalingservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SessionRegistryService {

    // roomId → WebSocket 세션 목록
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> roomSessions
            = new ConcurrentHashMap<>();

    // sessionId → roomId 역매핑
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();

    public void addSession(String roomId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(session);
        sessionRoomMap.put(session.getId(), roomId);
    }

    public String removeSession(WebSocketSession session) {
        String roomId = sessionRoomMap.remove(session.getId());
        if (roomId != null) {
            CopyOnWriteArrayList<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomId);
                }
            }
        }
        return roomId;
    }

    public String getRoomIdBySession(WebSocketSession session) {
        return sessionRoomMap.get(session.getId());
    }

    public List<WebSocketSession> getSessions(String roomId) {
        return roomSessions.getOrDefault(roomId, new CopyOnWriteArrayList<>());
    }

    public boolean hasLocalSessions(String roomId) {
        CopyOnWriteArrayList<WebSocketSession> sessions = roomSessions.get(roomId);
        return sessions != null && !sessions.isEmpty();
    }
}
