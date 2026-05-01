package com.mohit.telecom.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpgradeRequest {

    @NotNull(message = "New plan ID is required")
    private Long newPlanId;
}
