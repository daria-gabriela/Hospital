package main.java.hospital.service;

import main.java.hospital.model.*;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class ConsultationService {

    private final List<Consultation> consultations;
    private final MedicalRecordService medicalRecordService;

    // Constructor implicit (folosit în mod normal în aplicație)
    public ConsultationService() {
        this.consultations = new ArrayList<>();
        this.medicalRecordService = new MedicalRecordService();
        loadFromDatabase();
    }

    // Constructor alternativ dacă ai deja instanță de MedicalRecordService
    public ConsultationService(MedicalRecordService medicalRecordService) {
        this.consultations = new ArrayList<>();
        this.medicalRecordService = medicalRecordService;
        loadFromDatabase();
    }

    // Metodă care încarcă toate consultațiile din BD
    private void loadFromDatabase() {
        consultations.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM consultations");
             ResultSet rs = stmt.executeQuery()) {

            // Încarcă tot ResultSet-ul în memorie (lista de map-uri)
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }

            // Încarcă serviciile DOAR după închiderea ResultSet-ului
            PatientService patientService = new PatientService();
            DoctorService doctorService = new DoctorService();
            DiagnosisService diagnosisService = new DiagnosisService();

            for (Map<String, Object> row : rows) {
                int id = (int) row.get("id");
                int patientId = (int) row.get("patient_id");
                int doctorId = (int) row.get("doctor_id");
                int diagnosisId = (int) row.get("diagnosis_id");
                String notes = (String) row.get("notes");
                LocalDate date = ((Date) row.get("date")).toLocalDate();

                Optional<Patient> patientOpt = patientService.getPatientById(patientId);
                Optional<Doctor> doctorOpt = doctorService.getDoctorById(doctorId);
                Optional<Diagnosis> diagnosisOpt = diagnosisService.getDiagnosisById(diagnosisId);

                if (patientOpt.isPresent() && doctorOpt.isPresent() && diagnosisOpt.isPresent()) {
                    Consultation c = new Consultation(patientOpt.get(), doctorOpt.get(), date, diagnosisOpt.get(), notes);
                    c.setId(id);
                    consultations.add(c);
                }
                else {
                    System.err.println("❌ Consultație cu ID " + id + " nu a putut fi încărcată: "
                            + "Pacient/Doctor/Diagnostic lipsă.");
                }

            }

            AuditService.getInstance().log("LOAD_ALL_CONSULTATIONS_FROM_DB");

        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcarea consultațiilor: " + e.getMessage());
        }

    }


    public Consultation createConsultation(Patient patient, Doctor doctor,
                                           LocalDate date, Diagnosis diagnosis, String notes) {

        int diagnosisId = getDiagnosisIdByName(diagnosis.getName());

        if (diagnosisId == -1) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO diagnoses (name, description, date, doctor_id, medical_record_id) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, diagnosis.getName());
                stmt.setString(2, diagnosis.getDescription());
                stmt.setDate(3, Date.valueOf(diagnosis.getDate()));
                stmt.setInt(4, doctor.getId());

                if (patient.getMedicalRecord() != null) {
                    stmt.setInt(5, patient.getMedicalRecord().getId());
                } else {
                    stmt.setNull(5, Types.INTEGER);
                }

                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    diagnosis.setId(rs.getInt(1));
                }

                System.out.println("➕ Diagnosticul a fost adăugat în baza de date.");
            } catch (SQLException e) {
                System.err.println("❌ Eroare JDBC la adăugarea diagnosticului: " + e.getMessage());
            }
        } else {
            diagnosis.setId(diagnosisId);
        }

        Consultation consultation = new Consultation(patient, doctor, date, diagnosis, notes);
        consultations.add(consultation);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO consultations (patient_id, doctor_id, date, diagnosis_id, notes) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patient.getId());
            stmt.setInt(2, doctor.getId());
            stmt.setDate(3, Date.valueOf(date));
            stmt.setInt(4, diagnosis.getId());
            stmt.setString(5, notes);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la inserare consultație: " + e.getMessage());
        }

        if (patient.getMedicalRecord() != null) {
            patient.getMedicalRecord().addDiagnosis(diagnosis);
            System.out.println("✅ Diagnosticul a fost atașat fișei medicale.");
        } else {
            System.out.println("⚠️ Pacientul nu are fișă medicală. Diagnosticul NU a fost salvat în istoric.");
        }

        AuditService.getInstance().log("CREATE_CONSULTATION: ID=" + consultation.getId());
        return consultation;
    }

    private int getDiagnosisIdByName(String name) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id FROM diagnoses WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la căutare diagnostic după nume: " + e.getMessage());
        }
        return -1;
    }

    public List<Consultation> getAllConsultations() {
        AuditService.getInstance().log("READ_ALL_CONSULTATIONS");
        return new ArrayList<>(consultations);
    }

    public void displayAllConsultations() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        if (consultations.isEmpty()) {
            System.out.println("📋 Nu există consultații înregistrate.");
        } else {
            System.out.println("=== Lista Consultațiilor ===");
            consultations.forEach(c -> {
                System.out.println(c);
                System.out.println("📅 Data consultației: " + c.getDate().format(formatter));
            });
        }
        AuditService.getInstance().log("DISPLAY_ALL_CONSULTATIONS");
    }

    public List<Consultation> getConsultationsForPatient(String cnp) {
        List<Consultation> list = consultations.stream()
                .filter(c -> c.getPatient().getCnp().equalsIgnoreCase(cnp))
                .collect(Collectors.toList());
        AuditService.getInstance().log("READ_CONSULTATIONS_FOR_PATIENT: " + cnp);
        return list;
    }

    public Optional<Consultation> getConsultationById(int id) {
        Optional<Consultation> result = consultations.stream().filter(c -> c.getId() == id).findFirst();
        AuditService.getInstance().log("READ_CONSULTATION_BY_ID: " + id);
        return result;
    }

    public boolean deleteConsultationById(int id) {
        Optional<Consultation> optional = getConsultationById(id);
        optional.ifPresent(consultations::remove);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM consultations WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la ștergere consultație: " + e.getMessage());
        }
        AuditService.getInstance().log(optional.isPresent() ? "DELETE_CONSULTATION: ID=" + id : "DELETE_CONSULTATION_FAILED: ID=" + id);
        return optional.isPresent();
    }

    public List<Consultation> getConsultationsByDate(LocalDate date) {
        List<Consultation> list = consultations.stream()
                .filter(c -> c.getDate().equals(date))
                .collect(Collectors.toList());
        AuditService.getInstance().log("READ_CONSULTATIONS_BY_DATE: " + date);
        return list;
    }

    public List<Consultation> getLastNConsultations(int n) {
        List<Consultation> list = consultations.stream()
                .sorted(Comparator.comparing(Consultation::getDate).reversed())
                .limit(n)
                .collect(Collectors.toList());
        AuditService.getInstance().log("READ_LAST_" + n + "_CONSULTATIONS");
        return list;
    }

    public void reportConsultationsPerDoctor() {
        System.out.println("📊 Număr de consultații per doctor:");
        consultations.stream()
                .collect(Collectors.groupingBy(c -> c.getDoctor().getFullName(), Collectors.counting()))
                .forEach((doctor, count) -> System.out.println("- " + doctor + ": " + count + " consultații"));
        AuditService.getInstance().log("REPORT_CONSULTATIONS_PER_DOCTOR");
    }

    public void reset() {
        consultations.clear();
        Consultation.resetIdCounter();
        AuditService.getInstance().log("RESET_ALL_CONSULTATIONS");
    }

    public void saveConsultation(Consultation consultation) {
        String updateSql = """
        UPDATE medical_appointments 
        SET patient_id = ?, doctor_id = ?, diagnosis_id = ?, date = ?, notes = ?
        WHERE id = ?
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {

            stmt.setInt(1, consultation.getPatient().getId());
            stmt.setInt(2, consultation.getDoctor().getId());
            stmt.setInt(3, consultation.getDiagnosis().getId());
            stmt.setDate(4, Date.valueOf(consultation.getDate()));
            stmt.setString(5, consultation.getNotes());
            stmt.setInt(6, consultation.getId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                System.out.println("❌ Nicio consultație nu a fost actualizată în baza de date.");
            } else {
                System.out.println("✅ Consultația a fost salvată în baza de date.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Eroare la salvarea consultației: " + e.getMessage());
        }
    }

}
