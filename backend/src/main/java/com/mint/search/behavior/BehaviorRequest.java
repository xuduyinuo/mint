package com.mint.search.behavior;

import lombok.Data;

@Data
public class BehaviorRequest {
    private String eventType;
    private String keyword;
    private String itemId;
    private String itemType;
    private String tags;
    private Integer durationSeconds;
}
