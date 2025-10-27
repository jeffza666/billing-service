package com.hospital.billing.controller;

import com.hospital.billing.dto.BillingRequest;
import com.hospital.billing.model.Billing;
import com.hospital.billing.model.BillingStatus;
import com.hospital.billing.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    
    private final BillingService billingService;
    
    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }
    
    @PostMapping
    public ResponseEntity<Billing> createBilling(@RequestBody BillingRequest request) {
        try {
            Billing billing = billingService.createBilling(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(billing);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{id}/pay")
    public ResponseEntity<Billing> processPayment(@PathVariable Long id) {
        try {
            Billing billing = billingService.processPayment(id);
            return ResponseEntity.ok(billing);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Billing> cancelBilling(@PathVariable Long id) {
        try {
            Billing billing = billingService.cancelBilling(id);
            return ResponseEntity.ok(billing);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Billing>> getAllBillings() {
        List<Billing> billings = billingService.getAllBillings();
        return ResponseEntity.ok(billings);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Billing> getBillingById(@PathVariable Long id) {
        return billingService.getBillingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Billing>> getBillingsByPatientId(@PathVariable String patientId) {
        List<Billing> billings = billingService.getBillingsByPatientId(patientId);
        return ResponseEntity.ok(billings);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Billing>> getBillingsByStatus(@PathVariable String status) {
        try {
            BillingStatus billingStatus = BillingStatus.valueOf(status.toUpperCase());
            List<Billing> billings = billingService.getBillingsByStatus(billingStatus);
            return ResponseEntity.ok(billings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}