package com.example.finalproject.controller.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RewardResponseDto {
    private Long rewardId;
    private String rewardName;
    private String rewardDescription;
    private String mentation;
    private boolean isGold;
    private boolean isActive;
}
