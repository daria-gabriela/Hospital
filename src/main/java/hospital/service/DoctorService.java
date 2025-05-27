package main.java.hospital.service;

import main.java.hospital.model.Doctor;
import main.java.hospital.model.Specialization;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorService {

    private final List<Doctor> doctors = new ArrayList<>();
    private final AuditService audit = AuditService.getInstance();

    public DoctorService() {
        loadDoctorsFromDB();
    }

    public void loadDoctorsFromDB() {
        doctors.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Doctor doctor = new Doctor(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        Specialization.valueOf(rs.getString("specialization")),
                        rs.getInt("years_of_experience"),
                        rs.getString("parafa_code"),
                        true
                );
                doctor.setId(rs.getInt("id"));
                doctors.add(doctor);
            }
            audit.log("LOAD_DOCTORS_FROM_DB");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare doctori: " + e.getMessage());
        }
    }

    public Optional<Doctor> getDoctorByName(String firstName, String lastName) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors WHERE first_name = ? AND last_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Doctor doctor = new Doctor(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        Specialization.valueOf(rs.getString("specialization")),
                        rs.getInt("years_of_experience"),
                        rs.getString("parafa_code"),
                        true
                );
                doctor.setId(rs.getInt("id"));
                return Optional.of(doctor);
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare la căutarea doctorului după nume: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Doctor addDoctor(String firstName, String lastName, String email, String phoneNumber,
                            Specialization specialization, int yearsOfExperience, String parafaCode) {
        Doctor doctor = new Doctor(firstName, lastName, email, phoneNumber, specialization, yearsOfExperience, parafaCode);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO doctors (first_name, last_name, email, phone_number, specialization, years_of_experience, parafa_code) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, phoneNumber);
            stmt.setString(5, specialization.name());
            stmt.setInt(6, yearsOfExperience);
            stmt.setString(7, parafaCode);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                doctor.setId(generatedId);
            }

            doctors.add(doctor);
            audit.log("CREATE_DOCTOR: " + parafaCode);
            System.out.println("✅ Doctor adăugat: " + doctor.getFullName() + " (ID: " + doctor.getId() + ")");
            return doctor;
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la adăugare doctor: " + e.getMessage());
        }
        return null;
    }

    public Optional<Doctor> getDoctorByParafaCode(String parafaCode) {
        Optional<Doctor> doctor = doctors.stream()
                .filter(d -> d.getParafaCode().equals(parafaCode))
                .findFirst();
        audit.log("READ_DOCTOR_BY_PARAFACODE: " + parafaCode);
        return doctor;
    }

    public List<Doctor> getAllDoctors() {
        audit.log("READ_ALL_DOCTORS");
        return new ArrayList<>(doctors);
    }

    public boolean updateDoctor(String parafaCode, Specialization newSpecialization, int newYearsOfExperience) {
        Optional<Doctor> optionalDoctor = getDoctorByParafaCode(parafaCode);
        if (optionalDoctor.isPresent()) {
            Doctor doctor = optionalDoctor.get();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE doctors SET specialization = ?, years_of_experience = ? WHERE parafa_code = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, newSpecialization.name());
                stmt.setInt(2, newYearsOfExperience);
                stmt.setString(3, parafaCode);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    doctor.setSpecialization(newSpecialization);
                    doctor.setYearsOfExperience(newYearsOfExperience);
                    audit.log("UPDATE_DOCTOR: " + parafaCode);
                    System.out.println("✏️ Doctor actualizat: " + doctor.getFullName());
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("❌ Eroare JDBC la actualizare doctor: " + e.getMessage());
            }
        }
        audit.log("UPDATE_DOCTOR_FAILED: " + parafaCode);
        System.out.println("⚠️ Doctorul cu parafa " + parafaCode + " nu a fost găsit.");
        return false;
    }

    public boolean changeParafaCode(String oldCode, String newCode) {
        Optional<Doctor> optionalDoctor = getDoctorByParafaCode(oldCode);
        if (optionalDoctor.isPresent()) {
            Doctor doctor = optionalDoctor.get();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE doctors SET parafa_code = ? WHERE parafa_code = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, newCode);
                stmt.setString(2, oldCode);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    doctor.setParafaCode(newCode);
                    doctors.remove(doctor);
                    doctor.setParafaCode(newCode);
                    doctors.add(doctor);
                    audit.log("CHANGE_PARAFACODE: " + oldCode + " -> " + newCode);
                    System.out.println("🔁 Codul parafei a fost schimbat pentru: " + doctor.getFullName());
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("❌ Eroare JDBC la schimbare parafa: " + e.getMessage());
            }
        }
        audit.log("CHANGE_PARAFACODE_FAILED: " + oldCode);
        System.out.println("⚠️ Codul parafei " + oldCode + " nu a fost găsit.");
        return false;
    }

    public boolean deleteDoctor(String parafaCode) {
        Optional<Doctor> optionalDoctor = getDoctorByParafaCode(parafaCode);
        if (optionalDoctor.isPresent()) {
            Doctor doctor = optionalDoctor.get();
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "DELETE FROM doctors WHERE parafa_code = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, parafaCode);
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    doctors.remove(doctor);
                    audit.log("DELETE_DOCTOR: " + parafaCode);
                    System.out.println("🗑️ Doctor șters: " + doctor.getFullName());
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("❌ Eroare JDBC la ștergere doctor: " + e.getMessage());
            }
        }
        audit.log("DELETE_DOCTOR_FAILED: " + parafaCode);
        System.out.println("⚠️ Doctorul cu parafa " + parafaCode + " nu a fost găsit.");
        return false;
    }

    public void displayAllDoctors() {
        if (doctors.isEmpty()) {
            System.out.println("📋 Nu există doctori înregistrați.");
        } else {
            System.out.println("=== Lista doctorilor ===");
            for (Doctor doctor : doctors) {
                System.out.println(doctor);
            }
        }
        audit.log("DISPLAY_ALL_DOCTORS");
    }

    public void loadFromDatabase() {
        doctors.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Doctor doctor = new Doctor(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        Specialization.valueOf(rs.getString("specialization")),
                        rs.getInt("years_of_experience"),
                        rs.getString("parafa_code"),
                        true
                );
                doctor.setId(rs.getInt("id"));
                doctors.add(doctor);
            }
            audit.log("LOAD_DOCTORS_FROM_DB");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare doctori: " + e.getMessage());
        }
    }
    public void updateDoctorPersonalInfo(Doctor doctor) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE doctors SET first_name = ?, last_name = ?, email = ?, phone_number = ? WHERE parafa_code = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, doctor.getFirstName());
            stmt.setString(2, doctor.getLastName());
            stmt.setString(3, doctor.getEmail());
            stmt.setString(4, doctor.getPhoneNumber());
            stmt.setString(5, doctor.getParafaCode());
            stmt.executeUpdate();

            audit.log("UPDATE_DOCTOR_PERSONAL_INFO: " + doctor.getParafaCode());
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la actualizare informații personale doctor: " + e.getMessage());
        }
    }

    public Optional<Doctor> getDoctorById(int doctorId) {

        for (Doctor doctor : doctors) {
            if (doctor.getId() == doctorId) {
                audit.log("READ_DOCTOR_BY_ID: " + doctorId);
                return Optional.of(doctor);
            }
        }
        audit.log("READ_DOCTOR_BY_ID_FAILED: " + doctorId);
        System.out.println("⚠️ Doctorul cu ID " + doctorId + " nu a fost găsit.");
        return Optional.empty();
    }
}