package com.mohit.telecom.subscription.repository;

import com.mohit.telecom.subscription.entity.Subscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SubscriptionRepositoryTest {

    @Autowired
    SubscriptionRepository subscriptionRepository;

    private Subscription saveSubscription(Long customerId, Long planId, String status) {
        Subscription s = new Subscription();
        s.setCustomerId(customerId);
        s.setPlanId(planId);
        s.setStatus(status);
        s.setStartDate(LocalDate.now());
        s.setEndDate(LocalDate.now().plusDays(30));
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return subscriptionRepository.save(s);
    }

    @Test
    void findByCustomerId_returnsAllSubscriptions() {
        saveSubscription(1L, 10L, "ACTIVE");
        saveSubscription(1L, 11L, "CANCELLED");

        List<Subscription> result = subscriptionRepository.findByCustomerId(1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void existsByCustomerIdAndPlanIdAndStatus_returnsTrueWhenMatch() {
        saveSubscription(2L, 20L, "ACTIVE");

        boolean exists = subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(2L, 20L, "ACTIVE");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByCustomerIdAndPlanIdAndStatus_returnsFalseWhenNoMatch() {
        saveSubscription(3L, 30L, "CANCELLED");

        boolean exists = subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(3L, 30L, "ACTIVE");
        assertThat(exists).isFalse();
    }
}
