package com.mohit.telecom.subscription.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionResponse {
    private Long subscriptionId;
    private Long customerId;
    private Long planId;
    private String planName;
    private String planType;
    private BigDecimal planPrice;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
