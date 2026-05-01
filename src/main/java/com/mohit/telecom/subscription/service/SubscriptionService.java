package com.mohit.telecom.subscription.service;

import com.mohit.telecom.subscription.dto.request.SubscribeRequest;
import com.mohit.telecom.subscription.dto.request.UpgradeRequest;
import com.mohit.telecom.subscription.dto.response.SubscriptionResponse;
import com.mohit.telecom.subscription.entity.Plan;
import com.mohit.telecom.subscription.entity.Subscription;
import com.mohit.telecom.subscription.exception.TelecomException;
import com.mohit.telecom.subscription.repository.CustomerRepository;
import com.mohit.telecom.subscription.repository.PlanRepository;
import com.mohit.telecom.subscription.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core business logic for telecom plan subscriptions.
 * Handles the full subscription lifecycle: subscribe, upgrade, cancel.
 */
@Service
public class SubscriptionService {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    PlanRepository planRepository;

    @Autowired
    CustomerRepository customerRepository;

    /**
     * Subscribes a customer to a telecom plan.
     * Validates plan existence, customer existence, and prevents duplicate active subscriptions.
     */
    public SubscriptionResponse subscribe(Long customerId, SubscribeRequest request) {
        LOG.info("Subscribing customer {} to plan {}", customerId, request.getPlanId());

        // verify customer exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "CUST-001", "Customer not found: " + customerId));

        Plan plan = planRepository.findByIdAndActiveTrue(request.getPlanId())
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "PLAN-001", "Plan not found or inactive: " + request.getPlanId()));

        // check if customer already has an active subscription to this exact plan
        boolean alreadySubscribed = subscriptionRepository
                .existsByCustomerIdAndPlanIdAndStatus(customerId, request.getPlanId(), "ACTIVE");
        if (alreadySubscribed) {
            throw new TelecomException(HttpStatus.CONFLICT, "SUB-001", "Customer already has an active subscription to this plan");
        }

        Subscription subscription = new Subscription();
        subscription.setCustomerId(customerId);
        subscription.setPlanId(plan.getId());
        subscription.setStatus("ACTIVE");
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getValidityDays()));
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());

        Subscription saved = subscriptionRepository.save(subscription);
        LOG.info("Subscription {} created for customer {} on plan {}", saved.getId(), customerId, plan.getId());

        return buildResponse(saved, plan);
    }

    public SubscriptionResponse upgrade(Long subscriptionId, UpgradeRequest request) {
        LOG.info("Upgrading subscription {} to plan {}", subscriptionId, request.getNewPlanId());

        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "SUB-002", "Subscription not found: " + subscriptionId));

        if (!"ACTIVE".equals(sub.getStatus())) {
            throw new TelecomException(HttpStatus.BAD_REQUEST, "SUB-003",
                    "Only active subscriptions can be upgraded. Current status: " + sub.getStatus());
        }

        if (sub.getPlanId().equals(request.getNewPlanId())) {
            throw new TelecomException(HttpStatus.BAD_REQUEST, "SUB-004", "Already subscribed to this plan");
        }

        Plan newPlan = planRepository.findByIdAndActiveTrue(request.getNewPlanId())
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "PLAN-001", "Target plan not found or inactive"));

        sub.setPlanId(newPlan.getId());
        sub.setEndDate(LocalDate.now().plusDays(newPlan.getValidityDays()));
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);

        // TODO: publish upgrade event for billing adjustment

        return buildResponse(sub, newPlan);
    }

    public void cancel(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new TelecomException(HttpStatus.NOT_FOUND, "SUB-002", "Subscription not found: " + subscriptionId));

        if ("CANCELLED".equals(sub.getStatus())) {
            throw new TelecomException(HttpStatus.BAD_REQUEST, "SUB-005", "Subscription is already cancelled");
        }

        sub.setStatus("CANCELLED");
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
        LOG.info("Subscription {} cancelled", subscriptionId);
    }

    public List<SubscriptionResponse> getSubscriptionsForCustomer(Long customerId) {
        List<Subscription> subs = subscriptionRepository.findByCustomerId(customerId);
        return subs.stream().map(s -> {
            Plan plan = planRepository.findById(s.getPlanId()).orElse(null);
            return buildResponse(s, plan);
        }).collect(Collectors.toList());
    }

    private SubscriptionResponse buildResponse(Subscription s, Plan plan) {
        return SubscriptionResponse.builder()
                .subscriptionId(s.getId())
                .customerId(s.getCustomerId())
                .planId(s.getPlanId())
                .planName(plan != null ? plan.getName() : "Unknown")
                .planType(plan != null ? plan.getPlanType() : null)
                .planPrice(plan != null ? plan.getPrice() : null)
                .status(s.getStatus())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
