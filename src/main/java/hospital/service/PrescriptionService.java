package main.java.hospital.service;

import main.java.hospital.model.Prescription;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PrescriptionService {

    private final List<Prescription> prescriptions = new ArrayList<>();

    // === Încărcare inițială din baza de date ===
    public void loadFromDatabase() {
        prescriptions.clear();
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM prescriptions");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Prescription p = new Prescription(
                        rs.getString("medication"),
                        rs.getString("dosage"),
                        rs.getDate("date_issued") != null ? rs.getDate("date_issued").toLocalDate() : LocalDate.now(),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getBoolean("auto_renew"),
                        rs.getDate("renew_date") != null ? rs.getDate("renew_date").toLocalDate() : null
                );
                p.setId(rs.getInt("id"));
                p.setDiagnosisId(rs.getInt("diagnosis_id"));
                prescriptions.add(p);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Eroare la încărcarea rețetelor din DB: " + e.getMessage());
        }
    }

    public void addPrescription(Prescription prescription) throws SQLException {
        if (prescription == null) {
            throw new IllegalArgumentException("Rețeta nu poate fi null.");
        }

        Connection conn = DatabaseConnection.getInstance().getConnection();
        String sql = "INSERT INTO prescriptions (medication, dosage, start_date, end_date, date_issued, auto_renew, renew_date, diagnosis_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, prescription.getMedication());
        stmt.setString(2, prescription.getDosage());
        stmt.setDate(3, Date.valueOf(prescription.getStartDate()));
        stmt.setDate(4, Date.valueOf(prescription.getEndDate()));
        stmt.setDate(5, Date.valueOf(prescription.getDateIssued()));
        stmt.setBoolean(6, prescription.isAutoRenew());

        if (prescription.getRenewDate() != null) {
            stmt.setDate(7, Date.valueOf(prescription.getRenewDate()));
        } else {
            stmt.setNull(7, Types.DATE);
        }

        stmt.setInt(8, prescription.getDiagnosisId());

        stmt.executeUpdate();

        // Obține ID-ul generat de MySQL
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            int generatedId = generatedKeys.getInt(1);
            prescription.setId(generatedId);
        }

        stmt.close();

        // Adaugă în memorie după ce a fost inserat cu succes
        prescriptions.add(prescription);

        AuditService.getInstance().log("CREATE_PRESCRIPTION: ID=" + prescription.getId());
    }


    public List<Prescription> getAllPrescriptions() {
        AuditService.getInstance().log("READ_ALL_PRESCRIPTIONS");
        return new ArrayList<>(prescriptions);
    }

    public Optional<Prescription> findById(int id) {
        Optional<Prescription> result = prescriptions.stream().filter(p -> p.getId() == id).findFirst();
        AuditService.getInstance().log("READ_PRESCRIPTION_BY_ID: " + id);
        return result;
    }

    public boolean deletePrescription(int id) {
        Optional<Prescription> optional = findById(id);
        if (optional.isPresent()) {
            prescriptions.remove(optional.get());

            try {
                Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM prescriptions WHERE id = ?");
                stmt.setInt(1, id);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                System.err.println("❌ Eroare la ștergerea rețetei din DB: " + e.getMessage());
            }

            AuditService.getInstance().log("DELETE_PRESCRIPTION: ID=" + id);
            return true;
        }
        AuditService.getInstance().log("DELETE_PRESCRIPTION_FAILED: ID=" + id);
        return false;
    }

    public List<Prescription> getPrescriptionsToRenew(LocalDate date) {
        List<Prescription> result = prescriptions.stream()
                .filter(p -> p.isAutoRenew() && date.equals(p.getRenewDate()))
                .collect(Collectors.toList());
        AuditService.getInstance().log("GET_PRESCRIPTIONS_TO_RENEW_ON: " + date);
        return result;
    }

    public List<Prescription> getActivePrescriptions() {
        LocalDate today = LocalDate.now();
        List<Prescription> result = prescriptions.stream()
                .filter(p -> (p.getStartDate().isBefore(today) || p.getStartDate().isEqual(today)) &&
                        (p.getEndDate().isAfter(today) || p.getEndDate().isEqual(today)))
                .collect(Collectors.toList());
        AuditService.getInstance().log("READ_ACTIVE_PRESCRIPTIONS");
        return result;
    }

    public boolean updateDosage(int id, String newDosage) {
        Optional<Prescription> optional = findById(id);
        if (optional.isPresent()) {
            optional.get().setDosage(newDosage);

            try {
                Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement("UPDATE prescriptions SET dosage = ? WHERE id = ?");
                stmt.setString(1, newDosage);
                stmt.setInt(2, id);
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                System.err.println("❌ Eroare la actualizarea dozei în DB: " + e.getMessage());
            }

            AuditService.getInstance().log("UPDATE_DOSAGE: ID=" + id);
            return true;
        }
        AuditService.getInstance().log("UPDATE_DOSAGE_FAILED: ID=" + id);
        return false;
    }

    public void printAllPrescriptions() {
        if (prescriptions.isEmpty()) {
            System.out.println("📋 Nu există rețete înregistrate.");
        } else {
            System.out.println("=== Lista rețetelor înregistrate ===");
            for (Prescription p : prescriptions) {
                System.out.println(p);
                System.out.println("-----------------------------");
            }
        }
        AuditService.getInstance().log("DISPLAY_ALL_PRESCRIPTIONS");
    }

    public void searchByMedication(String name) {
        var results = prescriptions.stream()
                .filter(p -> p.getMedication().equalsIgnoreCase(name))
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            System.out.println("🔍 Nicio rețetă găsită pentru: " + name);
        } else {
            System.out.println("=== Rețete pentru medicația: " + name + " ===");
            results.forEach(System.out::println);
        }
        AuditService.getInstance().log("SEARCH_PRESCRIPTION_BY_MEDICATION: " + name);
    }

    public void displayPrescriptionById(int id) {
        Optional<Prescription> optional = findById(id);
        optional.ifPresentOrElse(
                p -> System.out.println("📄 Rețetă găsită: \n" + p),
                () -> System.out.println("❌ Rețeta cu ID " + id + " nu a fost găsită.")
        );
        AuditService.getInstance().log("DISPLAY_PRESCRIPTION_BY_ID: " + id);
    }
    public void reloadPrescriptions() {
        loadFromDatabase();
    }
    public List<Prescription> getPrescriptionsByDiagnosisId(int diagnosisId) {
        List<Prescription> result = prescriptions.stream()
                .filter(p -> p.getDiagnosisId() == diagnosisId)
                .collect(Collectors.toList());

        AuditService.getInstance().log("GET_PRESCRIPTIONS_BY_DIAGNOSIS_ID: " + diagnosisId);
        return result;
    }


}
