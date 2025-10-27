package com.hospital.billing.dto;

public class PaymentMessage {
    private String patientId;
    private String patientName;
    private boolean paymentStatus;

    public PaymentMessage() {
    }

    public PaymentMessage(String patientId, String patientName, boolean paymentStatus) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.paymentStatus = paymentStatus;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public boolean isPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(boolean paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String toString() {
        return "PaymentMessage{" +
                "patientId='" + patientId + '\'' +
                ", patientName='" + patientName + '\'' +
                ", paymentStatus=" + paymentStatus +
                '}';
    }
}