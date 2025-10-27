package com.hospital.billing.kafka;

import com.hospital.billing.dto.PaymentMessage;
import com.hospital.billing.dto.PrescriptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topic.payment}")
    private String paymentTopic;
    
    @Value("${kafka.topic.prescription}")
    private String prescriptionTopic;
    
    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendPaymentMessage(PaymentMessage paymentMessage) {
        logger.info("Sending payment message: {}", paymentMessage);
        kafkaTemplate.send(paymentTopic, paymentMessage);
    }
    
    public void sendPrescriptionMessage(PrescriptionMessage prescriptionMessage) {
        logger.info("Sending prescription message: {}", prescriptionMessage);
        kafkaTemplate.send(prescriptionTopic, prescriptionMessage);
    }
}