package com.mohit.telecom.subscription.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlanRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    @NotBlank(message = "Plan type is required")
    @Pattern(regexp = "PREPAID|POSTPAID|DATA", message = "Plan type must be PREPAID, POSTPAID, or DATA")
    private String planType;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private Integer dataLimitMb;

    @NotNull(message = "Validity days is required")
    @Min(value = 1, message = "Validity must be at least 1 day")
    private Integer validityDays;

    private String description;
}
