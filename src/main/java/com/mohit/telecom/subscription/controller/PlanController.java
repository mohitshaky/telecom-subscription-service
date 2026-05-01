package com.mohit.telecom.subscription.controller;

import com.mohit.telecom.subscription.dto.request.PlanRequest;
import com.mohit.telecom.subscription.dto.response.PlanResponse;
import com.mohit.telecom.subscription.service.PlanService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for telecom plan management.
 * GET endpoints are accessible to all authenticated users.
 * Write operations (create, update, deactivate) are restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Plans", description = "Browse and manage telecom plans")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    @Autowired
    private PlanService planService;

    @GetMapping
    @Operation(
        summary = "List all active plans",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID for tracing", example = "txn-abc-123"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID for distributed tracing"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Originating channel (WEB, MOBILE, API)", example = "WEB"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant identifier for multi-tenancy", example = "TENANT-01")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plans retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PlanResponse>> getAllPlans(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(planService.getPlansByType(type));
        }
        return ResponseEntity.ok(planService.getAllActivePlans());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get plan by ID",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Source channel"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan found"),
        @ApiResponse(responseCode = "404", description = "Plan not found")
    })
    public ResponseEntity<PlanResponse> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create a new plan (Admin only)",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Source channel"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Plan created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update a plan (Admin only)",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Source channel"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Plan updated"),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public ResponseEntity<PlanResponse> updatePlan(@PathVariable Long id,
                                                    @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Deactivate a plan (Admin only) - soft delete",
        parameters = {
            @Parameter(name = "X-Transaction-Id", in = ParameterIn.HEADER, description = "Unique transaction ID"),
            @Parameter(name = "X-Correlation-Id", in = ParameterIn.HEADER, description = "Correlation ID"),
            @Parameter(name = "X-Source-Channel", in = ParameterIn.HEADER, description = "Source channel"),
            @Parameter(name = "X-Tenant-Id", in = ParameterIn.HEADER, description = "Tenant ID")
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Plan deactivated"),
        @ApiResponse(responseCode = "404", description = "Plan not found"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public ResponseEntity<Void> deactivatePlan(@PathVariable Long id) {
        planService.deactivatePlan(id);
        return ResponseEntity.noContent().build();
    }
}
