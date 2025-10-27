package com.hospital.billing.service;

import com.hospital.billing.dto.BillingRequest;
import com.hospital.billing.dto.PaymentMessage;
import com.hospital.billing.dto.PrescriptionMessage;
import com.hospital.billing.kafka.KafkaProducer;
import com.hospital.billing.model.Billing;
import com.hospital.billing.model.BillingStatus;
import com.hospital.billing.repository.BillingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingService.class);
    
    private final BillingRepository billingRepository;
    private final KafkaProducer kafkaProducer;
    
    public BillingService(BillingRepository billingRepository, KafkaProducer kafkaProducer) {
        this.billingRepository = billingRepository;
        this.kafkaProducer = kafkaProducer;
    }
    
    @Transactional
    public Billing createBilling(BillingRequest request) {
        logger.info("Creating billing for patient: {}, medicine: {}", 
                    request.getPatientName(), request.getMedicineName());
        
        // Calculate total amount
        BigDecimal totalAmount = request.getPricePerUnit()
                                        .multiply(BigDecimal.valueOf(request.getQuantity()));
        
        Billing billing = new Billing();
        billing.setPatientId(request.getPatientId());
        billing.setPatientName(request.getPatientName());
        billing.setMedicineId(request.getMedicineId());
        billing.setMedicineName(request.getMedicineName());
        billing.setQuantity(request.getQuantity());
        billing.setPricePerUnit(request.getPricePerUnit());
        billing.setTotalAmount(totalAmount);
        billing.setStatus(BillingStatus.PENDING);
        billing.setBillingDate(LocalDateTime.now());
        
        return billingRepository.save(billing);
    }
    
    @Transactional
    public Billing processPayment(Long billingId) {
        logger.info("Processing payment for billing ID: {}", billingId);
        
        Optional<Billing> billingOpt = billingRepository.findById(billingId);
        
        if (billingOpt.isEmpty()) {
            logger.error("Billing with ID {} not found", billingId);
            throw new IllegalArgumentException("Billing not found");
        }
        
        Billing billing = billingOpt.get();
        
        if (billing.getStatus() == BillingStatus.PAID) {
            logger.warn("Billing {} is already paid", billingId);
            return billing;
        }
        
        // Update billing status to PAID
        billing.setStatus(BillingStatus.PAID);
        billing.setPaymentDate(LocalDateTime.now());
        
        // Save updated billing
        Billing savedBilling = billingRepository.save(billing);
        
        // Send payment message to Pharmacy Service
        PaymentMessage paymentMessage = new PaymentMessage(
            savedBilling.getPatientId(),
            savedBilling.getPatientName(),
            true // payment status = true
        );
        kafkaProducer.sendPaymentMessage(paymentMessage);
        
        // Send prescription message to Pharmacy Service
        PrescriptionMessage prescriptionMessage = new PrescriptionMessage(
            savedBilling.getPatientId(),
            savedBilling.getMedicineId(),
            savedBilling.getMedicineName(),
            savedBilling.getQuantity()
        );
        kafkaProducer.sendPrescriptionMessage(prescriptionMessage);
        
        logger.info("Payment processed successfully for billing ID: {}", billingId);
        return savedBilling;
    }
    
    @Transactional
    public Billing cancelBilling(Long billingId) {
        logger.info("Cancelling billing with ID: {}", billingId);
        
        Optional<Billing> billingOpt = billingRepository.findById(billingId);
        
        if (billingOpt.isEmpty()) {
            logger.error("Billing with ID {} not found", billingId);
            throw new IllegalArgumentException("Billing not found");
        }
        
        Billing billing = billingOpt.get();
        
        if (billing.getStatus() == BillingStatus.PAID) {
            logger.error("Cannot cancel billing {} because it's already paid", billingId);
            throw new IllegalStateException("Cannot cancel paid billing");
        }
        
        billing.setStatus(BillingStatus.CANCELLED);
        
        return billingRepository.save(billing);
    }
    
    public List<Billing> getAllBillings() {
        return billingRepository.findAll();
    }
    
    public Optional<Billing> getBillingById(Long id) {
        return billingRepository.findById(id);
    }
    
    public List<Billing> getBillingsByPatientId(String patientId) {
        return billingRepository.findByPatientId(patientId);
    }
    
    public List<Billing> getBillingsByStatus(BillingStatus status) {
        return billingRepository.findByStatus(status);
    }
}