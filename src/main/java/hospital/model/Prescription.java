package main.java.hospital.model;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clasa Prescription reprezintă o rețetă medicală emisă unui pacient.
 * Conține informații despre medicamente, dozaj, perioada tratamentului
 * și opțional detalii privind reînnoirea automată.
 */
public class Prescription {

    // === Generator de ID unic pentru fiecare rețetă ===
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    // === Atribute ===
    private int id;                     // ID unic al rețetei
    private String medication;          // Numele medicamentului
    private String dosage;              // Dozajul prescris
    private LocalDate dateIssued;      // Data la care a fost emisă rețeta
    private LocalDate startDate;       // Începutul tratamentului
    private LocalDate endDate;         // Sfârșitul tratamentului
    private boolean autoRenew;         // Flag: se reînnoiește automat?
    private LocalDate renewDate;       // Data reînnoirii (dacă este cazul)
    private int diagnosisId;           // ID-ul diagnosticului asociat

    /**
     * Constructor complet. Validează perioada tratamentului și opțional reînnoirea.
     *
     * @param medication  Medicamentul prescris
     * @param dosage      Dozajul
     * @param dateIssued  Data eliberării rețetei
     * @param startDate   Începutul tratamentului
     * @param endDate     Sfârșitul tratamentului
     * @param autoRenew   True dacă se reînnoiește automat
     * @param renewDate   Data reînnoirii (obligatorie dacă autoRenew = true)
     */
    public Prescription(String medication, String dosage, LocalDate dateIssued,
                        LocalDate startDate, LocalDate endDate,
                        boolean autoRenew, LocalDate renewDate) {

        this.id = idCounter.getAndIncrement();

        setMedication(medication);
        setDosage(dosage);
        setDateIssued(dateIssued);
        setStartDate(startDate);
        setEndDate(endDate);
        setAutoRenew(autoRenew);
        setRenewDate(renewDate);

        validateTreatmentPeriod();
        validateAutoRenew();
    }

    // === Validări interne ===

    /**
     * Verifică dacă perioada tratamentului este logică (start < end).
     */
    private void validateTreatmentPeriod() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Data de sfârșit a tratamentului nu poate fi înainte de data de început.");
        }
    }

    /**
     * Verifică dacă renewDate este setat corect când autoRenew este activ.
     */
    private void validateAutoRenew() {
        if (autoRenew && renewDate == null) {
            throw new IllegalArgumentException("Rețeta este setată pentru reînnoire automată, dar nu are renewDate.");
        }
    }

    // === Getteri ===

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMedication() {
        return medication;
    }

    public String getDosage() {
        return dosage;
    }

    public LocalDate getDateIssued() {
        return dateIssued;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public LocalDate getRenewDate() {
        return renewDate;
    }

    public int getDiagnosisId() {
        return diagnosisId;
    }

    // === Setteri ===

    public void setMedication(String medication) {
        if (medication == null || medication.isBlank()) {
            throw new IllegalArgumentException("Medicamentul nu poate fi gol.");
        }
        this.medication = medication.trim();
    }

    public void setDosage(String dosage) {
        if (dosage == null || dosage.isBlank()) {
            throw new IllegalArgumentException("Dozajul nu poate fi gol.");
        }
        this.dosage = dosage.trim();
    }

    public void setDateIssued(LocalDate dateIssued) {
        if (dateIssued == null) {
            throw new IllegalArgumentException("Data eliberării rețetei nu poate fi null.");
        }
        this.dateIssued = dateIssued;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Data de început nu poate fi null.");
        }
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("Data de sfârșit nu poate fi null.");
        }
        this.endDate = endDate;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public void setRenewDate(LocalDate renewDate) {
        this.renewDate = renewDate;
    }

    public void setDiagnosisId(int diagnosisId) {
        this.diagnosisId = diagnosisId;
    }

    // === Afișare text ===

    @Override
    public String toString() {
        return "Prescription {\n" +
                "  ID: " + id + "\n" +
                "  Medicament: '" + medication + "'\n" +
                "  Dozaj: '" + dosage + "'\n" +
                "  Data eliberare: " + dateIssued + "\n" +
                "  Perioadă tratament: " + startDate + " - " + endDate + "\n" +
                "  Reînnoire automată: " + (autoRenew ? "Da (la " + renewDate + ")" : "Nu") + "\n" +
                "  Diagnostic ID: " + diagnosisId + "\n" +
                '}';
    }
}