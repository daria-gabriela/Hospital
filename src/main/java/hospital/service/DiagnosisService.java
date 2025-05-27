package main.java.hospital.service;

import main.java.hospital.model.Diagnosis;
import main.java.hospital.model.Doctor;
import main.java.hospital.model.Prescription;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class DiagnosisService {

    private final List<Diagnosis> diagnoses = new ArrayList<>();

    public void loadFromDatabase(List<Doctor> allDoctors) {
        diagnoses.clear();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM diagnoses");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String desc = rs.getString("description");
                LocalDate date = rs.getDate("date").toLocalDate();
                int doctorId = rs.getInt("doctor_id");
                int medicalRecordId = rs.getInt("medical_record_id");
                int diagnosisId = rs.getInt("id");

                Doctor doctor = allDoctors.stream()
                        .filter(d -> d.getId() == doctorId)
                        .findFirst()
                        .orElse(null);

                Diagnosis d = new Diagnosis(name, desc, date, doctor, medicalRecordId);
                d.setId(diagnosisId);

                try (PreparedStatement pStmt = conn.prepareStatement("SELECT * FROM prescriptions WHERE diagnosis_id = ?")) {
                    pStmt.setInt(1, diagnosisId);
                    ResultSet prs = pStmt.executeQuery();

                    while (prs.next()) {
                        Prescription p = new Prescription(
                                prs.getString("medication"),
                                prs.getString("dosage"),
                                prs.getDate("date_issued").toLocalDate(),
                                prs.getDate("start_date").toLocalDate(),
                                prs.getDate("end_date").toLocalDate(),
                                prs.getBoolean("auto_renew"),
                                prs.getDate("renew_date") != null ? prs.getDate("renew_date").toLocalDate() : null
                        );
                        p.setId(prs.getInt("id"));
                        p.setDiagnosisId(diagnosisId);
                        d.addPrescription(p);
                    }
                }

                diagnoses.add(d);
            }

            AuditService.getInstance().log("LOAD_ALL_DIAGNOSES_FROM_DB");

        } catch (SQLException e) {
            System.err.println("❌ Eroare la încărcarea diagnosticelor: " + e.getMessage());
        }
    }

    public Diagnosis addDiagnosis(String name, String description, LocalDate date, Doctor doctor, int medicalRecordId) {
        Diagnosis diagnosis = new Diagnosis(name.trim(), description, date, doctor, medicalRecordId);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String sql = "INSERT INTO diagnoses (name, description, date, doctor_id, medical_record_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, diagnosis.getName());
            stmt.setString(2, diagnosis.getDescription());
            stmt.setDate(3, Date.valueOf(diagnosis.getDate()));
            if (doctor != null) {
                stmt.setInt(4, doctor.getId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.setInt(5, medicalRecordId);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                diagnosis.setId(rs.getInt(1));
            }

            stmt.close();
            AuditService.getInstance().log("CREATE_DIAGNOSIS: " + name);
            diagnoses.add(diagnosis);
            System.out.println("✅ Diagnostic salvat în DB pentru fișa ID: " + medicalRecordId);

        } catch (SQLException e) {
            System.err.println("❌ Eroare la salvarea diagnosticului în DB: " + e.getMessage());
        }

        return diagnosis;
    }

    public boolean updateDiagnosisNameById(int id, String newName) {
        Optional<Diagnosis> optional = diagnoses.stream().filter(d -> d.getId() == id).findFirst();
        if (optional.isEmpty()) {
            System.out.println("❌ Diagnosticul cu ID " + id + " nu a fost găsit în memorie.");
            return false;
        }

        Diagnosis diagnosis = optional.get();
        diagnosis.setName(newName);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String sql = "UPDATE diagnoses SET name = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newName);
            stmt.setInt(2, id);

            int rows = stmt.executeUpdate();
            stmt.close();

            if (rows > 0) {
                AuditService.getInstance().log("UPDATE_DIAGNOSIS_NAME_BY_ID: ID=" + id + " -> " + newName);
                return true;
            } else {
                System.out.println("❌ Nicio înregistrare modificată. Verifică dacă ID-ul există.");
            }

        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC: " + e.getMessage());
        }

        return false;
    }

    public Optional<Diagnosis> findDiagnosisByName(String name) {
        return diagnoses.stream()
                .filter(d -> d.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public boolean deleteDiagnosis(String name) {
        Optional<Diagnosis> optional = findDiagnosisByName(name);
        if (optional.isPresent()) {
            diagnoses.remove(optional.get());
            try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM diagnoses WHERE name = ?");
                stmt.setString(1, name);
                stmt.executeUpdate();
                stmt.close();
                AuditService.getInstance().log("DELETE_DIAGNOSIS_DB: " + name);
                return true;
            } catch (SQLException e) {
                System.err.println("❌ Eroare la ștergerea din DB: " + e.getMessage());
            }
        }
        return false;
    }

    public boolean updateDiagnosisInDatabase(String oldName, String newDescription, LocalDate newDate) {
        Optional<Diagnosis> optional = findDiagnosisByName(oldName);
        if (optional.isEmpty()) return false;

        Diagnosis diagnosis = optional.get();
        if (newDescription != null && !newDescription.isBlank()) {
            diagnosis.setDescription(newDescription);
        }
        if (newDate != null) {
            diagnosis.setDate(newDate);
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String sql = "UPDATE diagnoses SET description = ?, date = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, diagnosis.getDescription());
            stmt.setDate(2, Date.valueOf(diagnosis.getDate()));
            stmt.setInt(3, diagnosis.getId());
            int rows = stmt.executeUpdate();
            stmt.close();
            AuditService.getInstance().log("UPDATE_DIAGNOSIS_DB: ID=" + diagnosis.getId());
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("❌ Eroare la actualizarea în DB: " + e.getMessage());
        }
        return false;
    }

    public boolean updateDiagnosisDoctor(String diagnosisName, Doctor newDoctor) {
        Optional<Diagnosis> optional = findDiagnosisByName(diagnosisName);
        if (optional.isEmpty()) return false;

        Diagnosis diagnosis = optional.get();
        diagnosis.setDoctor(newDoctor);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE diagnoses SET doctor_id = ? WHERE id = ?");
            if (newDoctor != null) {
                stmt.setInt(1, newDoctor.getId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, diagnosis.getId());
            int rows = stmt.executeUpdate();
            stmt.close();
            AuditService.getInstance().log("UPDATE_DIAGNOSIS_DOCTOR: ID=" + diagnosis.getId());
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("❌ Eroare la actualizarea doctorului: " + e.getMessage());
        }
        return false;
    }

    public void displayPrescriptionsForDiagnosis(String diagnosisName) {
        Optional<Diagnosis> optional = findDiagnosisByName(diagnosisName);
        if (optional.isPresent()) {
            List<Prescription> prescriptions = optional.get().getPrescriptions();
            if (prescriptions.isEmpty()) {
                System.out.println("📭 Nicio prescripție asociată cu acest diagnostic.");
            } else {
                prescriptions.forEach(System.out::println);
            }
        } else {
            System.out.println("❌ Diagnosticul nu a fost găsit.");
        }
        AuditService.getInstance().log("DISPLAY_PRESCRIPTIONS_FOR_DIAGNOSIS: " + diagnosisName);
    }

    public List<Diagnosis> getAllDiagnoses() {
        return new ArrayList<>(diagnoses);
    }

    public List<Diagnosis> getDiagnosesByMedicalRecordId(int medicalRecordId) {
        return diagnoses.stream()
                .filter(d -> d.getMedicalRecordId() == medicalRecordId)
                .toList();
    }

    public Optional<Diagnosis> getDiagnosisById(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM diagnoses WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Diagnosis diagnosis = new Diagnosis(
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDate("date").toLocalDate(),
                        null, // Doctor will be set later
                        rs.getInt("medical_record_id")
                );

                diagnosis.setId(rs.getInt("id"));
                return Optional.of(diagnosis);
            }

        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC în getDiagnosisById: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean addPrescriptionToDiagnosis(Diagnosis diagnosis, Prescription prescription, PrescriptionService prescriptionService) {
        if (diagnosis == null || prescription == null) return false;

        try {
            prescription.setDiagnosisId(diagnosis.getId());
            prescriptionService.addPrescription(prescription);
            diagnosis.addPrescription(prescription);
            prescriptionService.reloadPrescriptions();
            AuditService.getInstance().log("ADD_PRESCRIPTION_TO_DIAGNOSIS: ID=" + diagnosis.getId());
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Eroare la adăugarea rețetei în DB: " + e.getMessage());
            return false;
        }
    }


}
