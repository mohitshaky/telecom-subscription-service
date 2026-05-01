package com.mohit.telecom.subscription.controller;

import com.mohit.telecom.subscription.dto.request.SubscribeRequest;
import com.mohit.telecom.subscription.dto.request.UpgradeRequest;
import com.mohit.telecom.subscription.dto.response.SubscriptionResponse;
import com.mohit.telecom.subscription.entity.Customer;
import com.mohit.telecom.subscription.exception.TelecomException;
import com.mohit.telecom.subscription.repository.CustomerRepository;
import com.mohit.telecom.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for subscription lifecycle operations.
 * Customers can only manage their own subscriptions.
 */
@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscriptions", description = "Manage telecom plan subscriptions")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CustomerRepository customerRepository;

    private Long resolveCustomerId(UserDetails userDetails) {
        Customer customer = customerRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "CUST-001", "Customer not found"));
        return customer.getId();
    }

    @PostMapping
    @Operation(
        summary = "Subscribe to a telecom plan",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID for tracing", example = "txn-abc-123"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID for distributed tracing"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Originating channel (WEB, MOBILE, API)", example = "WEB"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant identifier", example = "TENANT-01")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Subscription created"),
        @ApiResponse(responseCode = "409", description = "Already subscribed to this plan"),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long customerId = resolveCustomerId(userDetails);
        SubscriptionResponse response = subscriptionService.subscribe(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{subscriptionId}/upgrade")
    @Operation(
        summary = "Upgrade an active subscription to a different plan",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Source channel"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription upgraded"),
        @ApiResponse(responseCode = "400", description = "Subscription not active or same plan"),
        @ApiResponse(responseCode = "404", description = "Subscription or plan not found")
    })
    public ResponseEntity<SubscriptionResponse> upgrade(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody UpgradeRequest request) {
        return ResponseEntity.ok(subscriptionService.upgrade(subscriptionId, request));
    }

    @DeleteMapping("/{subscriptionId}")
    @Operation(
        summary = "Cancel a subscription",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Source channel"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Subscription cancelled"),
        @ApiResponse(responseCode = "400", description = "Already cancelled"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public ResponseEntity<Void> cancel(@PathVariable Long subscriptionId) {
        subscriptionService.cancel(subscriptionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @Operation(
        summary = "Get all subscriptions for the current customer",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Source channel"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscriptions retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<SubscriptionResponse>> mySubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long customerId = resolveCustomerId(userDetails);
        return ResponseEntity.ok(subscriptionService.getSubscriptionsForCustomer(customerId));
    }
}
