package main.java.hospital.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Clasa Diagnosis reprezintă un diagnostic stabilit de un medic pentru un pacient.
 * Poate include prescripții asociate și conține informații complete despre diagnostic.
 */
public class Diagnosis {
    private static int nextId = 1;
    protected int id; // eliminat final pentru a permite setId
    private String name;
    private String description;
    private LocalDate date;
    private Doctor doctor;
    private final List<Prescription> prescriptions;
    private int medicalRecordId;

    public Diagnosis(String name, String description, LocalDate date, Doctor doctor, int medicalRecordId) {
        this.name = name;
        this.description = description;
        this.date = date;
        this.doctor = doctor;
        this.prescriptions = new ArrayList<>();
        this.id = nextId++;
        this.medicalRecordId = medicalRecordId;
    }

    public Diagnosis(String name, String description, LocalDate date, Doctor doctor, List<Prescription> prescriptions, int medicalRecordId) {
        this.name = name;
        this.description = description;
        this.date = date != null ? date : LocalDate.now();
        this.doctor = doctor;
        this.prescriptions = prescriptions != null ? prescriptions : new ArrayList<>();
        this.id = nextId++;
        this.medicalRecordId = medicalRecordId;
    }

    public void addPrescription(Prescription prescription) {
        if (prescription != null) {
            prescriptions.add(prescription);
        }
    }

    public void createAndAddPrescription(String medication, String dosage,
                                         LocalDate dateIssued, LocalDate startDate,
                                         LocalDate endDate, boolean autoRenew,
                                         LocalDate renewDate) {
        Prescription newPrescription = new Prescription(
                medication, dosage, dateIssued, startDate, endDate, autoRenew, renewDate
        );
        prescriptions.add(newPrescription);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null && !description.isBlank()) {
            this.description = description.trim();
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        if (date != null) {
            this.date = date;
        }
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        if (doctor != null) {
            this.doctor = doctor;
        }
    }

    public List<Prescription> getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(List<Prescription> newPrescriptions) {
        prescriptions.clear();
        if (newPrescriptions != null) {
            prescriptions.addAll(newPrescriptions);
        }
    }

    public int getMedicalRecordId() {
        return medicalRecordId;
    }

    public void setMedicalRecordId(int medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
    }

    public int getId() {
        return id;
    }

    public void setId(int diagnosisId) {
        this.id = diagnosisId;
    }

    public boolean removePrescriptionById(int id) {
        for (Prescription prescription : prescriptions) {
            if (prescription.getId() == id) {
                prescriptions.remove(prescription);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnosis {\n");
        sb.append("  Nume: ").append(name).append("\n");
        sb.append("  Descriere: ").append(description).append("\n");
        sb.append("  Data: ").append(date).append("\n");
        sb.append("  Medicul: ").append(doctor != null ? doctor.getFullName() : "N/A").append("\n");
        sb.append("  Parafa: ").append(doctor != null ? doctor.getParafaCode() : "N/A").append("\n");
        sb.append("  Fișă medicală ID: ").append(medicalRecordId).append("\n");
        sb.append("  Prescripții:");

        if (prescriptions.isEmpty()) {
            sb.append(" Nicio prescripție\n");
        } else {
            for (Prescription p : prescriptions) {
                sb.append("\n    - ").append(p);
            }
            sb.append("\n");
        }

        sb.append("}");
        return sb.toString();
    }
}
