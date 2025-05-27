package main.java.hospital.model;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class Patient extends Person {

    private static final Set<String> usedCnp = new HashSet<>();
    private static final String CNP_FILE_PATH = "data/used_cnps.txt";

    static {
        loadCnpsFromFile();
    }

    private String cnp;
    private String address;
    private BloodGroup bloodGroup;
    private RhType rhType;
    private MedicalRecord medicalRecord;
    private boolean active = true;
    private List<Invoice> invoices = new ArrayList<>();
    private List<MedicalAppointment> appointments = new ArrayList<>();

    private static void loadCnpsFromFile() {
        try {
            File file = new File(CNP_FILE_PATH);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    usedCnp.add(line.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Nu s-a putut încărca fișierul cu CNP-uri: " + e.getMessage());
        }
    }

    private static void saveCnpsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CNP_FILE_PATH))) {
            for (String cnp : usedCnp) {
                writer.write(cnp);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("⚠️ Eroare salvare fișier CNP: " + e.getMessage());
        }
    }

    public Patient(String firstName, String lastName, String email, String phoneNumber,
                   String cnp, String address, BloodGroup bloodGroup, RhType rhType,
                   boolean validateCnp) {
        super(firstName, lastName, email, phoneNumber);

        try {
            if (validateCnp) {
                validateCnp(cnp);
                usedCnp.add(cnp);
                saveCnpsToFile();
            }

            this.cnp = cnp;
            this.address = address;
            this.bloodGroup = bloodGroup;
            this.rhType = rhType;
            this.medicalRecord = new MedicalRecord(LocalDate.now());

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Eroare creare pacient: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Eroare neașteptată la creare pacient: " + e.getMessage());
            throw new IllegalStateException("Eroare la creare pacient", e);
        }
    }
    public Patient(int id, String firstName, String lastName, String email, String phoneNumber,
                   String cnp, String address, BloodGroup bloodGroup, RhType rhType,
                   boolean active, MedicalRecord medicalRecord) {
        super(firstName, lastName, email, phoneNumber);
        this.id = id;
        this.cnp = cnp;
        this.address = address;
        this.bloodGroup = bloodGroup;
        this.rhType = rhType;
        this.active = active;
        this.medicalRecord = medicalRecord;
    }


    public Patient(String firstName, String lastName) {
        super(firstName, lastName, null, null);
    }

    private void validateCnp(String cnp) {
        if (cnp == null || !cnp.matches("\\d{13}")) {
            throw new IllegalArgumentException("❌ CNP invalid: trebuie să conțină exact 13 cifre.");
        }
        if (!isValidCnpStructure(cnp)) {
            throw new IllegalArgumentException("❌ CNP invalid: cifra de control este incorectă.");
        }
        if (usedCnp.contains(cnp)) {
            throw new IllegalArgumentException("❌ CNP deja folosit: " + cnp);
        }
    }

    private boolean isValidCnpStructure(String cnp) {
        final int[] weights = {2, 7, 9, 1, 4, 6, 3, 5, 8, 2, 7, 9};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(cnp.charAt(i)) * weights[i];
        }
        int controlDigit = sum % 11;
        if (controlDigit == 10) controlDigit = 1;
        return controlDigit == Character.getNumericValue(cnp.charAt(12));
    }

    public int getAge() {
        try {
            String datePart = cnp.substring(1, 7);
            int year = Integer.parseInt(datePart.substring(0, 2));
            int month = Integer.parseInt(datePart.substring(2, 4));
            int day = Integer.parseInt(datePart.substring(4, 6));

            char centuryCode = cnp.charAt(0);
            int fullYear;

            switch (centuryCode) {
                case '1': case '2': fullYear = 1900 + year; break;
                case '3': case '4': fullYear = 1800 + year; break;
                case '5': case '6': fullYear = 2000 + year; break;
                case '7': case '8': fullYear = 2000 + year; break;
                default: throw new IllegalArgumentException("CNP invalid: cod secol necunoscut.");
            }

            LocalDate birthDate = LocalDate.of(fullYear, month, day);
            LocalDate now = LocalDate.now();

            return now.getYear() - birthDate.getYear() -
                    (now.getDayOfYear() < birthDate.getDayOfYear() ? 1 : 0);
        } catch (Exception e) {
            System.err.println("❌ Eroare la calculul vârstei: " + e.getMessage());
            return -1;
        }
    }

    public void deletePatient() {
        if (active) {
            usedCnp.remove(this.cnp);
            this.active = false;
            saveCnpsToFile();
        }
    }

    public boolean isActive() { return active; }
    public String getCnp() { return cnp; }
    public String getAddress() { return address; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public RhType getRhType() { return rhType; }
    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public void setAddress(String address) { this.address = address; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setRhType(RhType rhType) { this.rhType = rhType; }
    public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }

    public void addInvoice(Invoice invoice) {
        if (invoice != null) invoices.add(invoice);
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public List<Invoice> getUnpaidInvoices() {
        List<Invoice> result = new ArrayList<>();
        for (Invoice i : invoices) {
            if (!i.isPaid()) result.add(i);
        }
        return result;
    }

    public double getTotalDueAmount() {
        return invoices.stream().filter(i -> !i.isPaid()).mapToDouble(Invoice::getAmount).sum();
    }

    public List<MedicalAppointment> getAppointments() {
        return appointments;
    }

    public void addAppointment(MedicalAppointment appointment) {
        appointments.add(appointment);
    }

    public void removeAppointment(MedicalAppointment appointment) {
        appointments.remove(appointment);
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Pacient {" +
                "ID: " + id +
                ", Nume: " + getFullName() +
                ", CNP: " + cnp +
                ", Activ: " + active +
                ", Adresă: " + address +
                ", Grupa: " + bloodGroup +
                ", RH: " + rhType +
                '}';
    }

}
