package com.hospital.billing.repository;

import com.hospital.billing.model.Billing;
import com.hospital.billing.model.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingRepository extends JpaRepository<Billing, Long> {
    List<Billing> findByPatientId(String patientId);
    List<Billing> findByStatus(BillingStatus status);
}