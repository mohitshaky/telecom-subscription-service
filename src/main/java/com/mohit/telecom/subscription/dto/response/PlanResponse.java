package com.mohit.telecom.subscription.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PlanResponse {
    private Long id;
    private String name;
    private String planType;
    private BigDecimal price;
    private Integer dataLimitMb;
    private Integer validityDays;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
