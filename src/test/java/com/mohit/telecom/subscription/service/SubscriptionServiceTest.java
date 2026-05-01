package com.mohit.telecom.subscription.service;

import com.mohit.telecom.subscription.dto.request.SubscribeRequest;
import com.mohit.telecom.subscription.dto.request.UpgradeRequest;
import com.mohit.telecom.subscription.dto.response.SubscriptionResponse;
import com.mohit.telecom.subscription.entity.Customer;
import com.mohit.telecom.subscription.entity.Plan;
import com.mohit.telecom.subscription.entity.Subscription;
import com.mohit.telecom.subscription.exception.TelecomException;
import com.mohit.telecom.subscription.repository.CustomerRepository;
import com.mohit.telecom.subscription.repository.PlanRepository;
import com.mohit.telecom.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    SubscriptionRepository subscriptionRepository;
    @Mock
    PlanRepository planRepository;
    @Mock
    CustomerRepository customerRepository;

    @InjectMocks
    SubscriptionService subscriptionService;

    private Plan activePlan;
    private Customer customer;

    @BeforeEach
    void setUp() {
        activePlan = new Plan();
        activePlan.setId(1L);
        activePlan.setName("Basic Prepaid");
        activePlan.setPlanType("PREPAID");
        activePlan.setPrice(new BigDecimal("199.00"));
        activePlan.setValidityDays(30);
        activePlan.setActive(true);

        customer = new Customer();
        customer.setId(10L);
        customer.setEmail("test@example.com");
        customer.setRole("CUSTOMER");
        customer.setActive(true);
    }

    @Test
    void subscribe_success() {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(1L);

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(activePlan));
        when(subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(10L, 1L, "ACTIVE")).thenReturn(false);

        Subscription saved = new Subscription();
        saved.setId(100L);
        saved.setCustomerId(10L);
        saved.setPlanId(1L);
        saved.setStatus("ACTIVE");
        saved.setStartDate(LocalDate.now());
        saved.setEndDate(LocalDate.now().plusDays(30));
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(subscriptionRepository.save(any())).thenReturn(saved);

        SubscriptionResponse response = subscriptionService.subscribe(10L, request);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getPlanId()).isEqualTo(1L);
        assertThat(response.getCustomerId()).isEqualTo(10L);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void subscribe_duplicateActiveSub_throwsConflict() {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(1L);

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(planRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(activePlan));
        when(subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(10L, 1L, "ACTIVE")).thenReturn(true);

        TelecomException ex = catchThrowableOfType(
                () -> subscriptionService.subscribe(10L, request), TelecomException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getErrorCode()).isEqualTo("SUB-001");
    }

    @Test
    void subscribe_planNotFound_throwsNotFound() {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(99L);

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(planRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        TelecomException ex = catchThrowableOfType(
                () -> subscriptionService.subscribe(10L, request), TelecomException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo("PLAN-001");
    }

    @Test
    void upgrade_inactiveSubscription_throwsBadRequest() {
        Subscription sub = new Subscription();
        sub.setId(1L);
        sub.setPlanId(1L);
        sub.setStatus("CANCELLED");

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        UpgradeRequest request = new UpgradeRequest();
        request.setNewPlanId(2L);

        TelecomException ex = catchThrowableOfType(
                () -> subscriptionService.upgrade(1L, request), TelecomException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("SUB-003");
    }

    @Test
    void cancel_alreadyCancelled_throwsBadRequest() {
        Subscription sub = new Subscription();
        sub.setId(1L);
        sub.setStatus("CANCELLED");

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        TelecomException ex = catchThrowableOfType(
                () -> subscriptionService.cancel(1L), TelecomException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("SUB-005");
    }

    @Test
    void cancel_success() {
        Subscription sub = new Subscription();
        sub.setId(5L);
        sub.setStatus("ACTIVE");
        sub.setUpdatedAt(LocalDateTime.now());

        when(subscriptionRepository.findById(5L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenReturn(sub);

        subscriptionService.cancel(5L);

        assertThat(sub.getStatus()).isEqualTo("CANCELLED");
        verify(subscriptionRepository).save(sub);
    }
}
