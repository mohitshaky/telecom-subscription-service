package com.mohit.telecom.subscription.repository;

import com.mohit.telecom.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByCustomerId(Long customerId);

    Optional<Subscription> findByCustomerIdAndStatus(Long customerId, String status);

    boolean existsByCustomerIdAndPlanIdAndStatus(Long customerId, Long planId, String status);
}
