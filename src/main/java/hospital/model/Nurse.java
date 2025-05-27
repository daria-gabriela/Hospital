package main.java.hospital.model;

/**
 * Clasa Nurse reprezintă o asistentă medicală din cadrul spitalului.
 * Extinde clasa Person și include detalii precum tură, cod intern, certificări, experiență și disponibilitate.
 */
public class Nurse extends Person {

    private Shift shift;               // Tura de lucru: DAY sau NIGHT
    private String staffCode;          // Cod intern unic al asistentei
    private String certifications;     // Certificări (ex: "BLS, ACLS")
    private int yearsOfExperience;     // Experiență profesională în ani
    private boolean isOnCall;          // Disponibilitate pentru urgențe

    /**
     * Constructor complet pentru asistentă.
     */
    public Nurse(String firstName, String lastName, String email, String phoneNumber,
                 Shift shift, String staffCode, String certifications,
                 int yearsOfExperience, boolean isOnCall) {
        super(firstName, lastName, email, phoneNumber);
        this.shift = shift;
        this.staffCode = staffCode;
        this.certifications = certifications;
        this.yearsOfExperience = yearsOfExperience;
        this.isOnCall = isOnCall;
    }

    // === GETTERI și SETTERI ===

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        if (staffCode != null && !staffCode.isBlank()) {
            this.staffCode = staffCode;
        }
    }

    public String getCertifications() {
        return certifications;
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        if (yearsOfExperience >= 0) {
            this.yearsOfExperience = yearsOfExperience;
        }
    }

    public boolean isOnCall() {
        return isOnCall;
    }

    public void setOnCall(boolean onCall) {
        this.isOnCall = onCall;
    }

    /**
     * Afișare completă și detaliată a obiectului Nurse.
     */
    @Override
    public String toString() {
        return "👩‍⚕️ Asistentă {" +
                "ID: " + id +
                ", Nume: " + getFullName() +
                ", Email: " + email +
                ", Telefon: " + phoneNumber +
                ", Tură: " + shift +
                ", Cod intern: " + staffCode +
                ", Certificări: " + certifications +
                ", Experiență: " + yearsOfExperience + " ani" +
                ", Disponibilă la urgențe: " + (isOnCall ? "✔️ Da" : "❌ Nu") +
                '}';
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getFullName() {
        return super.getFullName(); // dacă `Person` o definește corect
    }


}
