package com.mohit.telecom.subscription.service;

import com.mohit.telecom.subscription.dto.request.PlanRequest;
import com.mohit.telecom.subscription.dto.response.PlanResponse;
import com.mohit.telecom.subscription.entity.Plan;
import com.mohit.telecom.subscription.exception.TelecomException;
import com.mohit.telecom.subscription.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    PlanRepository planRepository;

    @InjectMocks
    PlanService planService;

    private Plan buildPlan(Long id, String name, String type, boolean active) {
        Plan p = new Plan();
        p.setId(id);
        p.setName(name);
        p.setPlanType(type);
        p.setPrice(new BigDecimal("299.00"));
        p.setValidityDays(28);
        p.setActive(active);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    @Test
    void getAllActivePlans_returnsOnlyActive() {
        when(planRepository.findByActiveTrue()).thenReturn(List.of(
                buildPlan(1L, "Basic", "PREPAID", true),
                buildPlan(2L, "Pro", "POSTPAID", true)
        ));

        List<PlanResponse> result = planService.getAllActivePlans();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Basic");
    }

    @Test
    void createPlan_savesAndReturnsResponse() {
        PlanRequest request = new PlanRequest();
        request.setName("Data Plus");
        request.setPlanType("DATA");
        request.setPrice(new BigDecimal("499.00"));
        request.setValidityDays(60);

        Plan saved = buildPlan(10L, "Data Plus", "DATA", true);
        when(planRepository.save(any())).thenReturn(saved);

        PlanResponse response = planService.createPlan(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getPlanType()).isEqualTo("DATA");
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void getPlanById_notFound_throwsException() {
        when(planRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        TelecomException ex = catchThrowableOfType(
                () -> planService.getPlanById(99L), TelecomException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deactivatePlan_alreadyInactive_throwsBadRequest() {
        Plan plan = buildPlan(1L, "Old Plan", "PREPAID", false);
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        TelecomException ex = catchThrowableOfType(
                () -> planService.deactivatePlan(1L), TelecomException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getErrorCode()).isEqualTo("PLAN-002");
    }
}
