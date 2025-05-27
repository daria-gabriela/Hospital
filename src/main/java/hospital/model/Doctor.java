package main.java.hospital.model;

import java.util.HashSet;
import java.util.Set;

public class Doctor extends Person {

    private static final Set<String> usedParafaCodes = new HashSet<>();

    private Specialization specialization;
    private int yearsOfExperience;
    private String parafaCode;

    // ✅ Constructor pentru CREARE cu validare cod parafă
    public Doctor(String firstName, String lastName, String email, String phoneNumber,
                  Specialization specialization, int yearsOfExperience, String parafaCode) {
        super(firstName, lastName, email, phoneNumber);
        validateParafaCode(parafaCode);
        this.specialization = specialization;
        this.yearsOfExperience = yearsOfExperience;
        this.parafaCode = parafaCode;
        usedParafaCodes.add(parafaCode);
    }

    // ✅ Constructor pentru ÎNCĂRCARE din DB (fără validare)
    public Doctor(String firstName, String lastName, String email, String phoneNumber,
                  Specialization specialization, int yearsOfExperience, String parafaCode,
                  boolean skipValidation) {
        super(firstName, lastName, email, phoneNumber);
        this.specialization = specialization;
        this.yearsOfExperience = yearsOfExperience;
        this.parafaCode = parafaCode;

        if (!skipValidation) {
            validateParafaCode(parafaCode);
            usedParafaCodes.add(parafaCode);
        }
    }

    public Doctor(String firstname, String lastname) {
        super(firstname, lastname);
    }

    private void validateParafaCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Codul parafei nu poate fi null sau gol.");
        }
        if (usedParafaCodes.contains(code)) {
            throw new IllegalArgumentException("❌ Codul parafei este deja folosit: " + code);
        }
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getParafaCode() {
        return parafaCode;
    }

    public void setParafaCode(String newCode) {
        if (newCode == null || newCode.trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Codul parafei nu poate fi null sau gol.");
        }

        if (this.parafaCode.equals(newCode)) {
            // Nu schimbăm nimic dacă e același cod
            return;
        }

        if (usedParafaCodes.contains(newCode)) {
            throw new IllegalArgumentException("❌ Codul parafei este deja folosit: " + newCode);
        }

        usedParafaCodes.remove(this.parafaCode);
        usedParafaCodes.add(newCode);
        this.parafaCode = newCode;
    }


    public void releaseParafaCode() {
        usedParafaCodes.remove(this.parafaCode);
    }

    public int getId() {
        return super.getId();
    }

    public void setId(int generatedId) {
        if (generatedId <= 0) {
            throw new IllegalArgumentException("ID-ul trebuie să fie un număr pozitiv.");
        }
        this.id = generatedId;
    }

    @Override
    public String toString() {
        return "Doctor {" +
                "ID: " + id +
                ", Nume: " + getFullName() +
                ", Email: " + email +
                ", Telefon: " + phoneNumber +
                ", Specializare: " + specialization +
                ", Experiență: " + yearsOfExperience + " ani" +
                ", Cod parafă: " + parafaCode +
                '}';
    }
}
