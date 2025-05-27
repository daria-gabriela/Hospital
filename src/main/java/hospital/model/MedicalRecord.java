package main.java.hospital.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecord {
    private static int counter = 1; // pentru id local

    private int id;
    private LocalDate creationDate;
    private List<Diagnosis> diagnoses;

    // Constructor cu ID explicit (din DB)
    public MedicalRecord(int id, LocalDate creationDate) {
        this.id = id;
        this.creationDate = creationDate;
        this.diagnoses = new ArrayList<>();
    }

    // Constructor cu ID auto-generat (pentru test/local)
    public MedicalRecord(LocalDate creationDate) {
        this.id = generateId();
        this.creationDate = creationDate;
        this.diagnoses = new ArrayList<>();
    }

    private static int generateId() {
        return counter++;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public List<Diagnosis> getDiagnoses() {
        return diagnoses;
    }

    public void setDiagnoses(List<Diagnosis> diagnoses) {
        this.diagnoses = diagnoses;
    }

    public void addDiagnosis(Diagnosis diagnosis) {
        this.diagnoses.add(diagnosis);
    }

    @Override
    public String toString() {
        return "MedicalRecord {" +
                "id=" + id +
                ", creationDate=" + creationDate +
                ", diagnoses=" + diagnoses.size() +
                '}';
    }

    public boolean removeDiagnosisByName(String name) {

        return diagnoses.removeIf(d -> name.equalsIgnoreCase(d.getName()));
    }
}