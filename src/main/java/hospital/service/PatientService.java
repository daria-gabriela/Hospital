package main.java.hospital.service;

import main.java.hospital.model.*;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PatientService {
    private final List<Patient> patients;

    public PatientService() {
        this.patients = new ArrayList<>();
        loadPatientsFromDB();
    }

    private void loadPatientsFromDB() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.*, mr.id AS mr_id, mr.creation_date FROM patients p JOIN medical_records mr ON p.medical_record_id = mr.id";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MedicalRecord record = new MedicalRecord(
                        rs.getInt("mr_id"),
                        rs.getDate("creation_date").toLocalDate()
                );

                Patient patient = new Patient(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("cnp"),
                        rs.getString("address"),
                        BloodGroup.valueOf(rs.getString("blood_group")),
                        RhType.valueOf(rs.getString("rh_type")),
                        rs.getBoolean("active"),
                        record
                );

                patients.add(patient);
            }
            AuditService.getInstance().log("LOAD_PATIENTS_FROM_DB");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare pacienți: " + e.getMessage());
        }
    }


    public Patient addPatient(String firstName, String lastName, String email, String phoneNumber,
                              String cnp, String address, BloodGroup bloodGroup, RhType rhType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            int medicalRecordId;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO medical_records (creation_date) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                stmt.setDate(1, Date.valueOf(LocalDate.now()));
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    medicalRecordId = rs.getInt(1);
                } else {
                    conn.rollback();
                    throw new SQLException("Eroare la generarea ID-ului fișei medicale");
                }
            }

            String sql = "INSERT INTO patients (first_name, last_name, email, phone_number, cnp, address, blood_group, rh_type, medical_record_id, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setString(3, email);
                stmt.setString(4, phoneNumber);
                stmt.setString(5, cnp);
                stmt.setString(6, address);
                stmt.setString(7, bloodGroup.name());
                stmt.setString(8, rhType.name());
                stmt.setInt(9, medicalRecordId);
                stmt.setBoolean(10, true);
                stmt.executeUpdate();
            }

            Patient patient = new Patient(firstName, lastName, email, phoneNumber, cnp, address, bloodGroup, rhType, true);
            MedicalRecord record = new MedicalRecord(medicalRecordId, LocalDate.now());
            patient.setMedicalRecord(record);
            patients.add(patient);

            conn.commit();
            AuditService.getInstance().log("CREATE_PATIENT: " + cnp);
            AuditService.getInstance().log("CREATE_MEDICAL_RECORD_FOR_PATIENT: " + medicalRecordId + " - " + cnp);
            return patient;

        } catch (SQLException e) {
            System.err.println("❌ Eroare la adăugarea pacientului și fișei medicale: " + e.getMessage());
        }
        return null;
    }

    public boolean deletePatient(String cnp) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            Optional<Patient> optional = getPatientByCnp(cnp);
            if (optional.isEmpty()) return false;

            Patient patient = optional.get();
            int medicalRecordId = patient.getMedicalRecord().getId();

            try (PreparedStatement deletePrescriptions = conn.prepareStatement("DELETE FROM prescriptions WHERE diagnosis_id IN (SELECT id FROM diagnoses WHERE medical_record_id = ?)");
                 PreparedStatement deleteDiagnoses = conn.prepareStatement("DELETE FROM diagnoses WHERE medical_record_id = ?");
                 PreparedStatement deleteConsultations = conn.prepareStatement("DELETE FROM consultations WHERE patient_id = (SELECT id FROM patients WHERE cnp = ?)");
                 PreparedStatement deleteAppointments = conn.prepareStatement("DELETE FROM medical_appointments WHERE patient_id = (SELECT id FROM patients WHERE cnp = ?)");
                 PreparedStatement deleteInvoices = conn.prepareStatement("DELETE FROM invoices WHERE patient_id = (SELECT id FROM patients WHERE cnp = ?)");
                 PreparedStatement deletePatient = conn.prepareStatement("DELETE FROM patients WHERE cnp = ?");
                 PreparedStatement deleteRecord = conn.prepareStatement("DELETE FROM medical_records WHERE id = ?")
            ) {
                deletePrescriptions.setInt(1, medicalRecordId);
                deletePrescriptions.executeUpdate();

                deleteDiagnoses.setInt(1, medicalRecordId);
                deleteDiagnoses.executeUpdate();

                deleteConsultations.setString(1, cnp);
                deleteConsultations.executeUpdate();

                deleteAppointments.setString(1, cnp);
                deleteAppointments.executeUpdate();

                deleteInvoices.setString(1, cnp);
                deleteInvoices.executeUpdate();

                deletePatient.setString(1, cnp);
                deletePatient.executeUpdate();

                deleteRecord.setInt(1, medicalRecordId);
                deleteRecord.executeUpdate();
            }

            patients.removeIf(p -> p.getCnp().equals(cnp));
            conn.commit();
            AuditService.getInstance().log("DELETE_PATIENT_AND_RELATED_DATA: " + cnp);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Eroare la ștergerea pacientului și a datelor asociate: " + e.getMessage());
        }
        return false;
    }

    public Optional<Patient> getPatientByCnp(String cnp) {
        return patients.stream().filter(p -> p.getCnp().equals(cnp)).findFirst();
    }

    public List<Patient> getAllPatients() {
        return new ArrayList<>(patients);
    }

    public List<Patient> getActivePatients() {
        return patients.stream().filter(Patient::isActive).collect(Collectors.toList());
    }

    public MedicalRecord getMedicalRecordForPatient(String cnp) {
        return getPatientByCnp(cnp).map(Patient::getMedicalRecord).orElse(null);
    }

    public void displayAllPatients() {
        if (patients.isEmpty()) {
            System.out.println("📋 Nu există pacienți înregistrați.");
        } else {
            System.out.println("=== Lista pacienților înregistrați ===");
            for (Patient patient : patients) {
                System.out.println(patient + " (" + patient.getAge() + " ani)");
            }
        }
        AuditService.getInstance().log("DISPLAY_ALL_PATIENTS");
    }

    public void displayPatientByCnp() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();
        Optional<Patient> optional = getPatientByCnp(cnp);
        optional.ifPresentOrElse(
                p -> System.out.println("Pacient găsit: " + p + " (" + p.getAge() + " ani)"),
                () -> System.out.println("⚠️ Pacientul cu CNP " + cnp + " nu a fost găsit.")
        );
        AuditService.getInstance().log("DISPLAY_PATIENT_BY_CNP: " + cnp);
    }

    public void displayPatientsByAgeCategory() {
        Map<String, List<Patient>> categories = new HashMap<>();
        categories.put("children", new ArrayList<>());
        categories.put("adults", new ArrayList<>());
        categories.put("elderly", new ArrayList<>());

        for (Patient p : getActivePatients()) {
            int age = p.getAge();
            if (age < 18) categories.get("children").add(p);
            else if (age < 65) categories.get("adults").add(p);
            else categories.get("elderly").add(p);
        }

        categories.forEach((category, list) -> {
            System.out.println("=== " + category.toUpperCase() + " ===");
            list.stream().sorted(Comparator.comparingInt(Patient::getAge)).forEach(p ->
                    System.out.println(p + " (" + p.getAge() + " ani)")
            );
        });

        AuditService.getInstance().log("DISPLAY_PATIENTS_BY_AGE_CATEGORY");
    }

    public void updatePatientAddress(String cnp, String address) {
        updateField("UPDATE patients SET address = ? WHERE cnp = ?", address, cnp, "address", p -> p.setAddress(address));
    }

    public void updatePatientBloodGroup(String cnp, BloodGroup bg) {
        updateField("UPDATE patients SET blood_group = ? WHERE cnp = ?", bg.name(), cnp, "blood_group", p -> p.setBloodGroup(bg));
    }

    public void updatePatientRhType(String cnp, RhType rh) {
        updateField("UPDATE patients SET rh_type = ? WHERE cnp = ?", rh.name(), cnp, "rh_type", p -> p.setRhType(rh));
    }

    public void updatePhoneNumber(String cnp, String phone) {
        updateField("UPDATE patients SET phone_number = ? WHERE cnp = ?", phone, cnp, "phone_number", p -> p.setPhoneNumber(phone));
    }

    public void updatePatientEmail(String cnp, String email) {
        updateField("UPDATE patients SET email = ? WHERE cnp = ?", email, cnp, "email", p -> p.setEmail(email));
    }

    public void updatePatientName(String cnp, String lastName) {
        updateField("UPDATE patients SET last_name = ? WHERE cnp = ?", lastName, cnp, "last_name", p -> p.setLastName(lastName));
    }

    public void updatePatientFirstName(String cnp, String firstName) {
        updateField("UPDATE patients SET first_name = ? WHERE cnp = ?", firstName, cnp, "first_name", p -> p.setFirstName(firstName));
    }

    private boolean updateField(String sql, String value, String cnp, String column, java.util.function.Consumer<Patient> updater) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.setString(2, cnp);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                getPatientByCnp(cnp).ifPresent(updater);
                AuditService.getInstance().log("UPDATE_" + column.toUpperCase() + ": " + cnp);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC update " + column + ": " + e.getMessage());
        }
        return false;
    }

    public void displayInvoicesForPatient(String cnp) {
        Optional<Patient> optional = getPatientByCnp(cnp);
        if (optional.isPresent()) {
            List<Invoice> invoices = optional.get().getInvoices();
            System.out.println("📄 Facturi pentru pacientul " + optional.get().getFullName() + ":");
            invoices.forEach(System.out::println);
        } else {
            System.out.println("❌ Pacientul cu CNP " + cnp + " nu a fost găsit.");
        }
        AuditService.getInstance().log("DISPLAY_INVOICES_FOR_PATIENT: " + cnp);
    }

    public double getTotalUnpaidAmountForPatient(String cnp) {

        Optional<Patient> optional = getPatientByCnp(cnp);
        if (optional.isPresent()) {
            return optional.get().getInvoices().stream()
                    .filter(invoice -> !invoice.isPaid())
                    .mapToDouble(Invoice::getAmount)
                    .sum();
        }
        System.out.println("❌ Pacientul cu CNP " + cnp + " nu a fost găsit.");
        return 0.0;
    }
    public void linkMedicalRecords(MedicalRecordService medicalRecordService) {
        for (Patient patient : patients) {
            MedicalRecord record = patient.getMedicalRecord();
            if (record != null) {
                medicalRecordService.getMedicalRecordById(record.getId())
                        .ifPresent(patient::setMedicalRecord);
            }
        }
        AuditService.getInstance().log("LINK_PATIENTS_WITH_LOADED_MEDICAL_RECORDS");
    }
    public Optional<Patient> getPatientById(int id) {
        return patients.stream().filter(p -> p.getId() == id).findFirst();
    }

}
