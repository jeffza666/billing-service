package com.hospital.billing.dto;

public class PrescriptionMessage {
    private String patientId;
    private String medicineId;
    private String medicineName;
    private int quantity;

    public PrescriptionMessage() {
    }

    public PrescriptionMessage(String patientId, String medicineId, String medicineName, int quantity) {
        this.patientId = patientId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantity = quantity;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(String medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "PrescriptionMessage{" +
                "patientId='" + patientId + '\'' +
                ", medicineId='" + medicineId + '\'' +
                ", medicineName='" + medicineName + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}