package main.java.hospital.model;

import java.time.LocalDate;

/**
 * Clasa Invoice reprezintă o factură emisă pentru un pacient în urma unui serviciu medical.
 */
public class Invoice {
    private int id;
    private Patient patient;
    private double amount;
    private String description;
    private LocalDate date;
    private boolean isPaid;

    /**
     * Constructor folosit pentru facturi noi.
     */
    public Invoice(Patient patient, double amount, String description, LocalDate date, boolean isPaid) {
        this.patient = patient;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.isPaid = isPaid;

        if (patient != null) {
            patient.addInvoice(this); // Legătură în sens invers
        }
    }

    /**
     * Constructor folosit pentru încărcare din baza de date (cu ID).
     */
    public Invoice(int id, Patient patient, double amount, String description, LocalDate date, boolean isPaid) {
        this(patient, amount, description, date, isPaid);
        this.id = id;
    }

    // === GETTERS ===

    public int getInvoiceId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getPatientName() {
        return patient.getFullName();
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isPaid() {
        return isPaid;
    }

    // === SETTERS ===

    public void setInvoiceId(int id) {
        this.id = id;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setPaid(boolean paid) {
        this.isPaid = paid;
    }

    public void markAsPaid() {
        this.isPaid = true;
    }

    @Override
    public String toString() {
        return "Invoice {" +
                "ID: " + id +
                ", Pacient: " + patient.getFullName() +
                ", Suma: " + amount + " RON" +
                ", Descriere: '" + description + '\'' +
                ", Data: " + date +
                ", Plătită: " + (isPaid ? "DA" : "NU") +
                '}';
    }
}
