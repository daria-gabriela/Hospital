package main.java.hospital.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Reprezintă o consultație medicală între un pacient și un medic.
 * La crearea unei instanțe, diagnosticul este automat adăugat în istoricul medical al pacientului (dacă există).
 */
public class Consultation {
    private static int idCounter = 1; // Contor static pentru generarea automată a ID-urilor unice

    private  int id;             // ID unic al consultației (autogenerat)
    private Patient patient;          // Pacientul implicat în consultație
    private Doctor doctor;            // Doctorul care a efectuat consultația
    private LocalDate date;           // Data în care a avut loc consultația
    private Diagnosis diagnosis;      // Diagnosticul stabilit în urma consultației
    private String notes;             // Observații adiționale (opțional)

    /**
     * Constructor complet.
     * Verifică parametrii și adaugă diagnosticul în istoricul pacientului (dacă are un MedicalRecord și o programare activă).
     * După finalizare, camera folosită de programare se eliberează automat.
     *
     * @param patient    Pacientul consultat (obligatoriu, nu poate fi null)
     * @param doctor     Doctorul care efectuează consultația (obligatoriu)
     * @param date       Data consultației (obligatorie)
     * @param diagnosis  Diagnosticul pus (obligatoriu)
     * @param notes      Observații suplimentare (poate fi null)
     * @throws IllegalArgumentException dacă unul dintre parametrii obligatorii este null
     */
    public Consultation(Patient patient, Doctor doctor, LocalDate date, Diagnosis diagnosis, String notes) {
        if (patient == null || doctor == null || date == null || diagnosis == null) {
            throw new IllegalArgumentException("Parametrii patient, doctor, date și diagnosis nu pot fi null.");
        }

        this.id = idCounter++;
        this.patient = patient;
        this.doctor = doctor;
        this.date = date;
        this.diagnosis = diagnosis;
        this.notes = notes;

        // Adăugarea diagnosticului în istoricul medical al pacientului (dacă are medical record)
        if (patient.getMedicalRecord() != null) {
            patient.getMedicalRecord().addDiagnosis(diagnosis);
        }

        // Verificăm dacă pacientul are vreo programare activă și eliberăm camera dacă există
        List<MedicalAppointment> appointments = patient.getAppointments();
        for (MedicalAppointment appt : appointments) {
            if (appt.getDoctor().equals(doctor) && !appt.getDateTime().toLocalDate().isAfter(date)) {
                Room room = appt.getRoom();
                if (room != null && room.isOccupied()) {
                    room.free();
                }
            }
        }
    }

    // === Getteri și setteri ===

    /**
     * Returnează lista de programări asociate cu pacientul.
     * @return Lista de programări (poate fi goală, dar nu null)
     */
    public List<MedicalAppointment> getAppointments() {
        return patient.getAppointments(); // Retrieve appointments from the Patient class
    }

    public int getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(Diagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returnează o reprezentare textuală a consultației.
     * @return Rezumat al informațiilor despre consultație
     */
    @Override
    public String toString() {
        return "Consultation {" +
                "ID: " + id +
                ", Pacient: " + patient.getFirstName() + " " + patient.getLastName() +
                ", Doctor: " + doctor.getFirstName() + " " + doctor.getLastName() +
                ", Dată: " + date +
                ", Diagnostic: " + diagnosis +
                ", Observații: " + (notes != null ? notes : "N/A") +
                '}';
    }

    /**
     * Resetare a contorului de ID-uri (opțional – util pentru testare).
     */
    public static void resetIdCounter() {
        idCounter = 1;
    }


    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID-ul trebuie să fie un număr pozitiv.");
        }
        this.id = id;
    }
}
