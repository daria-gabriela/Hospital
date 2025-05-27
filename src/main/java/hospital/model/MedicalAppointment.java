package main.java.hospital.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clasa MedicalAppointment reprezintă o programare medicală între un pacient și un doctor.
 * Include data și ora programării, motivul și camera în care se desfășoară.
 */
public class MedicalAppointment {
    private static final AtomicInteger idCounter = new AtomicInteger(1); // Generator de ID-uri unice

    private final int id;                  // ID unic al programării (generat automat)
    private Patient patient;              // Pacientul programat
    private Doctor doctor;                // Doctorul responsabil
    private LocalDateTime dateTime;       // Data și ora programării
    private String reason;                // Motivul programării (ex: control, durere, tratament)
    private Room room;                    // Camera asociată programării

    /**
     * Constructor complet care generează automat un ID unic pentru programare
     * și setează camera ca ocupată.
     */
    public MedicalAppointment(Patient patient, Doctor doctor,
                              LocalDateTime dateTime, String reason, Room room) {
        this.id = idCounter.getAndIncrement();
        this.patient = patient;
        this.doctor = doctor;
        this.dateTime = dateTime;
        this.reason = reason;
        this.room = room;

        // Ocupăm camera cu ID-ul acestei programări
        if (room != null) {
            room.occupy(this.id);
        }
    }

    // === GETTERS ===

    public int getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getReason() {
        return reason;
    }

    public Room getRoom() {
        return room;
    }

    // === SETTERS ===

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Setează o nouă cameră pentru programare. Eliberează camera veche și ocupă camera nouă.
     */
    public void setRoom(Room room) {
        if (this.room != null) {
            this.room.free(); // Eliberează camera anterioară
        }
        this.room = room;
        if (room != null) {
            room.occupy(this.id); // Ocupă noua cameră
        }
    }

    /**
     * Returnează o reprezentare text a programării.
     */
    @Override
    public String toString() {
        return "Programare {" +
                "ID: " + id +
                ", Pacient: " + patient.getFirstName() + " " + patient.getLastName() +
                ", Doctor: " + doctor.getFirstName() + " " + doctor.getLastName() +
                ", Dată și oră: " + dateTime +
                ", Motiv: " + reason +
                ", Cameră: " + (room != null ? room.getRoomNumber() : "Nespecificată") +
                '}';
    }

}
