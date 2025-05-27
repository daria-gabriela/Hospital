package main.java.hospital.service;

import main.java.hospital.model.*;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MedicalAppointmentService {

    private final List<MedicalAppointment> appointments = new ArrayList<>();

    public MedicalAppointmentService() {
        loadFromDatabase();
    }


    public void loadFromDatabase() {
        appointments.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM medical_appointments")) {

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

            for (Map<String, Object> row : rows) {
                appointments.add(mapRowToAppointment(row));
            }

            AuditService.getInstance().log("LOAD_APPOINTMENTS_FROM_DB");
        } catch (SQLException e) {
            System.err.println("\u274C Eroare JDBC la loadFromDatabase: " + e.getMessage());
            AuditService.getInstance().log("LOAD_APPOINTMENTS_FAILED: " + e.getMessage());
        }
    }


    private Patient getPatientById(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM patients WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Patient p = new Patient(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("cnp"),
                        rs.getString("address"),
                        BloodGroup.valueOf(rs.getString("blood_group")),
                        RhType.valueOf(rs.getString("rh_type")),
                        false // persisted
                );
                p.setId(id);
                return p;
            }
        } catch (SQLException e) {
            System.err.println("\u274C Eroare JDBC la getPatientById: " + e.getMessage());
        }
        return null;
    }private MedicalAppointment mapRowToAppointment(Map<String, Object> row) {
        int id = (int) row.get("id");
        int patientId = (int) row.get("patient_id");
        int doctorId = (int) row.get("doctor_id");
        int roomNumber = (int) row.get("room_number");

        Object dateObj = row.get("date_time");
        LocalDateTime dateTime;
        if (dateObj instanceof Timestamp) {
            dateTime = ((Timestamp) dateObj).toLocalDateTime();
        } else if (dateObj instanceof LocalDateTime) {
            dateTime = (LocalDateTime) dateObj;
        } else {
            throw new IllegalArgumentException("Unsupported type for date_time: " + dateObj.getClass());
        }

        String reason = (String) row.get("reason");

        Patient patient = getPatientById(patientId);
        Doctor doctor = getDoctorById(doctorId);
        Room room = new Room(roomNumber, "Unknown Type", null, false);

        AuditService.getInstance().log("MAP_APPOINTMENT_ROW: id=" + id);
        return new MedicalAppointment(patient, doctor, dateTime, reason, room);
    }

    private Doctor getDoctorById(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM doctors WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Doctor doctor = new Doctor(
                        rs.getString("first_name"),
                        rs.getString("last_name")
                );
                doctor.setId(id);
                return doctor;
            }
        } catch (SQLException e) {
            System.err.println("\u274C Eroare JDBC la getDoctorById: " + e.getMessage());
        }
        return null;
    }


    public void addAppointment(MedicalAppointment appointment) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO medical_appointments (patient_id, doctor_id, room_number, date_time, reason) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, appointment.getPatient().getId());
            stmt.setInt(2, appointment.getDoctor().getId());
            if (appointment.getRoom() != null)
                stmt.setInt(3, appointment.getRoom().getRoomNumber());
            else
                stmt.setNull(3, Types.INTEGER);
            stmt.setTimestamp(4, Timestamp.valueOf(appointment.getDateTime()));
            stmt.setString(5, appointment.getReason());
            stmt.executeUpdate();
            AuditService.getInstance().log("ADD_APPOINTMENT: DB INSERT SUCCESS");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la inserare programare: " + e.getMessage());
        }
    }


    private MedicalAppointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int patientId = rs.getInt("patient_id");
        int doctorId = rs.getInt("doctor_id");
        int roomId = rs.getInt("room_number");
        LocalDateTime dateTime = rs.getTimestamp("date_time").toLocalDateTime();
        String reason = rs.getString("reason");

        Patient patient = getPatientById(patientId);
        Doctor doctor = getDoctorById(doctorId);
        Room room = new Room(roomId, "Unknown Type", null, false);

        return new MedicalAppointment(patient, doctor, dateTime, reason, room);
    }

    public List<MedicalAppointment> getAllAppointments() {
        List<MedicalAppointment> appointments = new ArrayList<>();

        Map<Integer, Patient> allPatients = loadAllPatients();
        Map<Integer, Doctor> allDoctors = loadAllDoctors();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM medical_appointments")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int patientId = rs.getInt("patient_id");
                int doctorId = rs.getInt("doctor_id");
                int roomNumber = rs.getInt("room_number");
                LocalDateTime dateTime = rs.getTimestamp("date_time").toLocalDateTime();
                String reason = rs.getString("reason");

                Patient patient = allPatients.get(patientId);
                Doctor doctor = allDoctors.get(doctorId);
                Room room = new Room(roomNumber, "Unknown", null, false);

                appointments.add(new MedicalAppointment( patient, doctor, dateTime, reason, room));
            }

            AuditService.getInstance().log("READ_ALL_APPOINTMENTS FROM DB");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la citire programări: " + e.getMessage());
        }

        return appointments;
    }


    public boolean deleteAppointment(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM medical_appointments WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            int deleted = stmt.executeUpdate();
            AuditService.getInstance().log("DELETE_APPOINTMENT: ID=" + id);
            return deleted > 0;
        } catch (SQLException e) {
            AuditService.getInstance().log("DELETE_APPOINTMENT_FAILED: ID=" + id);
            System.err.println("❌ Eroare JDBC la ștergere programare: " + e.getMessage());
            return false;
        }
    }

    public boolean updateAppointmentDateTime(int id, LocalDateTime newDateTime) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_appointments SET date_time = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setTimestamp(1, Timestamp.valueOf(newDateTime));
            stmt.setInt(2, id);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                AuditService.getInstance().log("UPDATE_APPOINTMENT_DATETIME: ID=" + id);
                System.out.println("✅ Dată/oră actualizată.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la actualizare dată/oră: " + e.getMessage());
        }
        return false;
    }

    public boolean updateAppointmentRoom(int id, Room roomId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_appointments SET room_number = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, roomId.getRoomNumber());
            stmt.setInt(2, id);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                AuditService.getInstance().log("UPDATE_APPOINTMENT_ROOM: ID=" + id);
                System.out.println("✅ Cameră actualizată.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la actualizare cameră: " + e.getMessage());
        }
        return false;
    }

    public boolean updateAppointmentNotes(int id, String newNotes) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_appointments SET reason = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newNotes);
            stmt.setInt(2, id);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                AuditService.getInstance().log("UPDATE_APPOINTMENT_NOTES: ID=" + id);
                System.out.println("✅ Observații actualizate.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la actualizare observații: " + e.getMessage());
        }
        return false;
    }

    public List<MedicalAppointment> getAppointmentsByPatientId(int patientId) {
        List<MedicalAppointment> appointments = new ArrayList<>();

        Patient patient = getPatientById(patientId);
        Map<Integer, Doctor> allDoctors = loadAllDoctors();

        if (patient == null) return appointments;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM medical_appointments WHERE patient_id = ?")) {

            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int doctorId = rs.getInt("doctor_id");
                int roomNumber = rs.getInt("room_number");
                LocalDateTime dateTime = rs.getTimestamp("date_time").toLocalDateTime();
                String reason = rs.getString("reason");

                Doctor doctor = allDoctors.get(doctorId);
                Room room = new Room(roomNumber, "Unknown", null, false);

                appointments.add(new MedicalAppointment( patient, doctor, dateTime, reason, room));
            }

            AuditService.getInstance().log("GET_APPOINTMENTS_BY_PATIENT_ID: " + patientId);
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la filtrare programări după pacient: " + e.getMessage());
        }

        return appointments;
    }

    public List<MedicalAppointment> getAppointmentsByDate(LocalDateTime date) {
        List<MedicalAppointment> appointments = new ArrayList<>();

        Map<Integer, Patient> allPatients = loadAllPatients();
        Map<Integer, Doctor> allDoctors = loadAllDoctors();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM medical_appointments WHERE DATE(date_time) = ?")) {

            stmt.setDate(1, java.sql.Date.valueOf(date.toLocalDate()));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int patientId = rs.getInt("patient_id");
                int doctorId = rs.getInt("doctor_id");
                int roomNumber = rs.getInt("room_number");
                LocalDateTime dateTime = rs.getTimestamp("date_time").toLocalDateTime();
                String reason = rs.getString("reason");

                Patient patient = allPatients.get(patientId);
                Doctor doctor = allDoctors.get(doctorId);
                Room room = new Room(roomNumber, "Unknown", null, false);

                appointments.add(new MedicalAppointment(patient, doctor, dateTime, reason, room));
            }

            AuditService.getInstance().log("GET_APPOINTMENTS_BY_DATE: " + date);
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la filtrare programări după dată: " + e.getMessage());
        }

        return appointments;
    }

    private Map<Integer, Doctor> loadAllDoctors() {

        Map<Integer, Doctor> doctors = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM doctors")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                Doctor doctor = new Doctor(
                        rs.getString("first_name"),
                        rs.getString("last_name")
                );
                doctor.setId(id);
                doctors.put(id, doctor);
            }
            AuditService.getInstance().log("LOAD_ALL_DOCTORS");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare doctori: " + e.getMessage());
        }
        return doctors;
    }

    private Map<Integer, Patient> loadAllPatients() {
        Map<Integer, Patient> patients = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM patients")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                Patient patient = new Patient(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getString("cnp"),
                        rs.getString("address"),
                        BloodGroup.valueOf(rs.getString("blood_group")),
                        RhType.valueOf(rs.getString("rh_type")),
                        false // persisted
                );
                patient.setId(id);
                patients.put(id, patient);
            }
            AuditService.getInstance().log("LOAD_ALL_PATIENTS");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare pacienți: " + e.getMessage());
        }
        return patients;
    }


    public boolean isDoctorAvailable(int doctorId, LocalDateTime dateTime) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM medical_appointments WHERE doctor_id = ? AND date_time = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctorId);
            stmt.setTimestamp(2, Timestamp.valueOf(dateTime));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean available = rs.getInt(1) == 0;
                AuditService.getInstance().log("CHECK_DOCTOR_AVAILABILITY: " + (available ? "AVAILABLE" : "NOT_AVAILABLE") + " -> doctorId=" + doctorId);
                return available;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la verificare disponibilitate medic: " + e.getMessage());
        }
        return false;
    }

    public void printAllAppointments() {
        List<MedicalAppointment> appointments = getAllAppointments();
        if (appointments.isEmpty()) {
            System.out.println("📭 Nu există programări înregistrate.");
        } else {
            appointments.forEach(System.out::println);
        }
        AuditService.getInstance().log("DISPLAY_ALL_APPOINTMENTS");
    }

    public Optional<MedicalAppointment> getAppointmentById(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM medical_appointments WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAppointment(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la getAppointmentById: " + e.getMessage());
        }
        return Optional.empty();
    }
}
