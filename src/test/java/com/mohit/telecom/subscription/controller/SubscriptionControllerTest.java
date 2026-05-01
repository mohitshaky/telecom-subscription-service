package com.mohit.telecom.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohit.telecom.subscription.dto.request.SubscribeRequest;
import com.mohit.telecom.subscription.dto.response.SubscriptionResponse;
import com.mohit.telecom.subscription.entity.Customer;
import com.mohit.telecom.subscription.exception.TelecomException;
import com.mohit.telecom.subscription.repository.CustomerRepository;
import com.mohit.telecom.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SubscriptionService subscriptionService;

    @MockBean
    CustomerRepository customerRepository;

    private Customer mockCustomer() {
        Customer c = new Customer();
        c.setId(1L);
        c.setEmail("user@example.com");
        c.setRole("CUSTOMER");
        c.setActive(true);
        return c;
    }

    private SubscriptionResponse mockResponse() {
        return SubscriptionResponse.builder()
                .subscriptionId(100L)
                .customerId(1L)
                .planId(1L)
                .planName("Basic Prepaid")
                .planType("PREPAID")
                .planPrice(new BigDecimal("199.00"))
                .status("ACTIVE")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "CUSTOMER")
    void subscribe_success_returns201() throws Exception {
        when(customerRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockCustomer()));
        when(subscriptionService.subscribe(eq(1L), any())).thenReturn(mockResponse());

        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(1L);

        mockMvc.perform(post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.planId").value(1));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "CUSTOMER")
    void subscribe_conflict_returns409() throws Exception {
        when(customerRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockCustomer()));
        when(subscriptionService.subscribe(eq(1L), any()))
                .thenThrow(new TelecomException(HttpStatus.CONFLICT, "SUB-001", "Already subscribed"));

        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(1L);

        mockMvc.perform(post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SUB-001"));
    }

    @Test
    void subscribe_unauthenticated_returns401() throws Exception {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(1L);

        mockMvc.perform(post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "CUSTOMER")
    void mySubscriptions_returnsListForCustomer() throws Exception {
        when(customerRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockCustomer()));
        when(subscriptionService.getSubscriptionsForCustomer(1L)).thenReturn(List.of(mockResponse()));

        mockMvc.perform(get("/api/subscriptions/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subscriptionId").value(100));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "CUSTOMER")
    void cancel_notFound_returns404() throws Exception {
        doThrow(new TelecomException(HttpStatus.NOT_FOUND, "SUB-002", "Subscription not found"))
                .when(subscriptionService).cancel(999L);

        mockMvc.perform(delete("/api/subscriptions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SUB-002"));
    }
}
