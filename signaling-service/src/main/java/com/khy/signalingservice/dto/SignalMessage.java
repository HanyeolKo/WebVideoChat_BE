package com.khy.signalingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalMessage {
    private String roomId;
    private String senderId;
    private String payload;
}
