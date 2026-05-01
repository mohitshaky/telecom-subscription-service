package com.mohit.telecom.subscription.service;

import com.mohit.telecom.subscription.dto.request.PlanRequest;
import com.mohit.telecom.subscription.dto.response.PlanResponse;
import com.mohit.telecom.subscription.entity.Plan;
import com.mohit.telecom.subscription.exception.TelecomException;
import com.mohit.telecom.subscription.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing telecom plans. Plan deactivation is soft-delete -
 * existing subscriptions referencing the plan remain intact.
 *
 * TODO: add caching for frequently accessed plans
 */
@Service
public class PlanService {

    private static final Logger LOG = LoggerFactory.getLogger(PlanService.class);

    @Autowired
    PlanRepository planRepository;

    public List<PlanResponse> getAllActivePlans() {
        return planRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PlanResponse> getPlansByType(String planType) {
        return planRepository.findByPlanTypeAndActiveTrue(planType.toUpperCase()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PlanResponse getPlanById(Long id) {
        Plan plan = planRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "PLAN-001", "Plan not found: " + id));
        return toResponse(plan);
    }

    public PlanResponse createPlan(PlanRequest request) {
        LOG.info("Creating new {} plan: {}", request.getPlanType(), request.getName());

        Plan plan = new Plan();
        plan.setName(request.getName());
        plan.setPlanType(request.getPlanType().toUpperCase());
        plan.setPrice(request.getPrice());
        plan.setDataLimitMb(request.getDataLimitMb());
        plan.setValidityDays(request.getValidityDays());
        plan.setDescription(request.getDescription());
        plan.setActive(true);

        Plan saved = planRepository.save(plan);
        LOG.info("Plan created with id: {}", saved.getId());
        return toResponse(saved);
    }

    public PlanResponse updatePlan(Long id, PlanRequest request) {
        Plan existingPlan = planRepository.findById(id)
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "PLAN-001", "Plan not found: " + id));

        existingPlan.setName(request.getName());
        existingPlan.setPlanType(request.getPlanType().toUpperCase());
        existingPlan.setPrice(request.getPrice());
        existingPlan.setDataLimitMb(request.getDataLimitMb());
        existingPlan.setValidityDays(request.getValidityDays());
        existingPlan.setDescription(request.getDescription());

        return toResponse(planRepository.save(existingPlan));
    }

    public void deactivatePlan(Long id) {
        Plan p = planRepository.findById(id)
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "PLAN-001", "Plan not found: " + id));

        if (!p.getActive()) {
            throw new TelecomException(HttpStatus.BAD_REQUEST, "PLAN-002", "Plan is already inactive");
        }

        p.setActive(false);
        planRepository.save(p);
        LOG.info("Plan {} deactivated", id);
    }

    private PlanResponse toResponse(Plan p) {
        return PlanResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .planType(p.getPlanType())
                .price(p.getPrice())
                .dataLimitMb(p.getDataLimitMb())
                .validityDays(p.getValidityDays())
                .description(p.getDescription())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
