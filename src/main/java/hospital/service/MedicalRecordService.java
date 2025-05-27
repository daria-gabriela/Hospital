package main.java.hospital.service;

import main.java.hospital.model.MedicalRecord;
import main.java.hospital.model.Diagnosis;
import main.java.hospital.model.Patient;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class MedicalRecordService {
    private final List<MedicalRecord> medicalRecords = new ArrayList<>();
    private final AuditService audit = AuditService.getInstance();

    public MedicalRecordService() {
        // Poți încărca fără diagnostice la inițializare
        loadMedicalRecordsFromDB();
    }

    public void loadMedicalRecordsFromDB() {
        medicalRecords.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM medical_records";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                LocalDate date = rs.getDate("creation_date").toLocalDate();
                MedicalRecord record = new MedicalRecord(id, date);
                medicalRecords.add(record);
            }
            audit.log("Încărcare fișe medicale din DB");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la citire fișe: " + e.getMessage());
        }
    }

    // ✅ Metoda nouă: încarcă și diagnosticele din DiagnosisService
    public void loadMedicalRecordsFromDB(DiagnosisService diagnosisService) {
        loadMedicalRecordsFromDB(); // încarcă fișele

        // pentru fiecare fișă, adaugă diagnosticele aferente
        for (MedicalRecord record : medicalRecords) {
            List<Diagnosis> diagnosesForRecord = diagnosisService.getDiagnosesByMedicalRecordId(record.getId());
            for (Diagnosis d : diagnosesForRecord) {
                record.addDiagnosis(d);
            }
        }

        audit.log("Încărcare fișe medicale + diagnostice din DB");
    }

    public MedicalRecord addMedicalRecord(LocalDate creationDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO medical_records (creation_date) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setDate(1, Date.valueOf(creationDate != null ? creationDate : LocalDate.now()));
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                MedicalRecord record = new MedicalRecord(id, creationDate != null ? creationDate : LocalDate.now());
                medicalRecords.add(record);
                audit.log("Adăugare fișă medicală ID: " + id);
                System.out.println("✅ Fișă medicală creată cu ID: " + id);
                return record;
            } else {
                System.err.println("❌ Nu s-a generat niciun ID pentru fișa medicală.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la inserare fișă: " + e.getMessage());
        }
        return null;
    }

    public Optional<MedicalRecord> getMedicalRecordById(int id) {
        audit.log("Căutare fișă medicală după ID: " + id);
        return medicalRecords.stream()
                .filter(record -> record.getId() == id)
                .findFirst();
    }

    public boolean updateMedicalRecordDate(int id, LocalDate newDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_records SET creation_date = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDate(1, Date.valueOf(newDate));
            stmt.setInt(2, id);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                for (MedicalRecord record : medicalRecords) {
                    if (record.getId() == id) {
                        record.setCreationDate(newDate);
                        break;
                    }
                }
                audit.log("Actualizare dată fișă medicală ID: " + id);
                System.out.println("✏️ Fișă actualizată în DB și în memorie.");
                return true;
            } else {
                System.out.println("⚠️ Nicio fișă nu a fost afectată în DB.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la actualizare fișă: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteMedicalRecord(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM medical_records WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                medicalRecords.removeIf(r -> r.getId() == id);
                audit.log("Ștergere fișă medicală ID: " + id);
                System.out.println("🗑️ Fișă ștearsă din DB și memorie.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la ștergere fișă: " + e.getMessage());
        }
        System.out.println("⚠️ Fișa nu a fost găsită.");
        return false;
    }

    public void displayAllMedicalRecords() {
        audit.log("Afișare toate fișele medicale");
        if (medicalRecords.isEmpty()) {
            System.out.println("📋 Nu există fișe medicale.");
            return;
        }

        for (MedicalRecord record : medicalRecords) {
            System.out.println(record);
            System.out.println("----------------------------------");
        }
    }

    public boolean isEmpty() {
        return medicalRecords.isEmpty();
    }
    /**
     * Caută în lista internă medicalRecords fișa asociată cu ID-ul
     * fișei din obiectul Patient și setează pacientului acea instanță.
     */
    public void linkPatientsWithRecords(List<Patient> patients) {
        for (Patient patient : patients) {
            MedicalRecord record = patient.getMedicalRecord();
            if (record != null) {
                getMedicalRecordById(record.getId())
                        .ifPresent(patient::setMedicalRecord); // 🔁 înlocuiește cu instanța completă
            }
        }
        audit.log("LINK_PATIENTS_WITH_LOADED_MEDICAL_RECORDS");
    }

}
