package com.khy.roomservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomSummaryResponse {
    private String id;
    private String title;
    private String content;
}
