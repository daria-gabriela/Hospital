package main.java.hospital.service;

import main.java.hospital.model.Nurse;
import main.java.hospital.model.Shift;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class NurseService {

    private final List<Nurse> nurses = new ArrayList<>();
    private final AuditService audit = AuditService.getInstance();

    public NurseService() {
        loadNursesFromDB();
    }

    public void loadNursesFromDB() {
        nurses.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM nurses";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Nurse nurse = new Nurse(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        Shift.valueOf(rs.getString("shift")),
                        rs.getString("staff_code"),
                        rs.getString("certifications"),
                        rs.getInt("years_of_experience"),
                        rs.getBoolean("is_on_call")
                );
                nurse.setId(rs.getInt("id"));
                nurses.add(nurse);
            }
            audit.log("LOAD_NURSES_FROM_DB");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare asistente: " + e.getMessage());
        }
    }

    public void addNurse(Nurse nurse) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO nurses (first_name, last_name, email, phone_number, certifications, years_of_experience, staff_code, shift, is_on_call) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, nurse.getFirstName());
            stmt.setString(2, nurse.getLastName());
            stmt.setString(3, nurse.getEmail());
            stmt.setString(4, nurse.getPhoneNumber());
            stmt.setString(5, nurse.getCertifications());
            stmt.setInt(6, nurse.getYearsOfExperience());
            stmt.setString(7, nurse.getStaffCode());
            stmt.setString(8, nurse.getShift().name());
            stmt.setBoolean(9, nurse.isOnCall());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                nurse.setId(rs.getInt(1));
                nurses.add(nurse);
                audit.log("Adăugare asistentă: " + nurse.getFullName());
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la adăugare asistentă: " + e.getMessage());
        }
    }

    public boolean updateNurseById(int id, Nurse updatedNurse) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE nurses SET first_name=?, last_name=?, email=?, phone_number=?, certifications=?, years_of_experience=?, staff_code=?, shift=?, is_on_call=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, updatedNurse.getFirstName());
            stmt.setString(2, updatedNurse.getLastName());
            stmt.setString(3, updatedNurse.getEmail());
            stmt.setString(4, updatedNurse.getPhoneNumber());
            stmt.setString(5, updatedNurse.getCertifications());
            stmt.setInt(6, updatedNurse.getYearsOfExperience());
            stmt.setString(7, updatedNurse.getStaffCode());
            stmt.setString(8, updatedNurse.getShift().name());
            stmt.setBoolean(9, updatedNurse.isOnCall());
            stmt.setInt(10, id);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                updatedNurse.setId(id);
                nurses.replaceAll(n -> n.getId() == id ? updatedNurse : n);
                audit.log("Actualizare completă asistentă ID: " + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la actualizare asistentă: " + e.getMessage());
        }
        audit.log("Eroare actualizare: asistentă inexistentă ID: " + id);
        return false;
    }

    public boolean removeNurseById(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM nurses WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                nurses.removeIf(n -> n.getId() == id);
                audit.log("Ștergere asistentă ID: " + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la ștergere asistentă: " + e.getMessage());
        }
        audit.log("Eroare ștergere asistentă ID: " + id);
        return false;
    }

    public Nurse getNurseById(int id) {
        audit.log("Căutare asistentă ID: " + id);
        return nurses.stream().filter(n -> n.getId() == id).findFirst().orElse(null);
    }

    public List<Nurse> getAllNurses() {
        audit.log("Accesare listă toate asistentele");
        return new ArrayList<>(nurses);
    }

    public boolean existsByStaffCode(String staffCode) {
        boolean exists = nurses.stream().anyMatch(n -> n.getStaffCode().equalsIgnoreCase(staffCode));
        audit.log("Verificare existență cod intern: " + staffCode + " => " + exists);
        return exists;
    }

    public Optional<Nurse> getByStaffCode(String staffCode) {
        Optional<Nurse> found = nurses.stream()
                .filter(n -> n.getStaffCode().equalsIgnoreCase(staffCode))
                .findFirst();
        audit.log("Căutare după cod intern: " + staffCode + " => " + (found.isPresent() ? "găsită" : "nu"));
        return found;
    }

    public List<Nurse> searchNursesByName(String name) {
        List<Nurse> result = nurses.stream()
                .filter(n -> n.getFullName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
        audit.log("Căutare asistente după nume: " + name);
        return result;
    }

    public List<Nurse> getNursesByShift(Shift shift) {
        List<Nurse> result = nurses.stream()
                .filter(n -> n.getShift() == shift)
                .collect(Collectors.toList());
        audit.log("Filtrare asistente pe tura: " + shift);
        return result;
    }

    public List<Nurse> getOnCallNurses() {
        List<Nurse> result = nurses.stream()
                .filter(Nurse::isOnCall)
                .collect(Collectors.toList());
        audit.log("Căutare asistente disponibile la urgențe");
        return result;
    }

    public List<Nurse> getNursesWithCertification(String cert) {
        List<Nurse> result = nurses.stream()
                .filter(n -> n.getCertifications().toLowerCase().contains(cert.toLowerCase()))
                .collect(Collectors.toList());
        audit.log("Filtrare asistente cu certificare: " + cert);
        return result;
    }

    public List<Nurse> getExperiencedNurses(int min) {
        List<Nurse> result = nurses.stream()
                .filter(n -> n.getYearsOfExperience() >= min)
                .collect(Collectors.toList());
        audit.log("Căutare asistente cu experiență minimă: " + min + " ani");
        return result;
    }

    public void editNurseById(int id, Scanner scanner) {
        Nurse nurse = getNurseById(id);
        if (nurse == null) {
            System.out.println("❌ Asistenta nu a fost găsită.");
            return;
        }

        int option;
        do {
            System.out.println("\n=== Editare Asistentă ID: " + id + " ===");
            System.out.println("1. Modifică prenume");
            System.out.println("2. Modifică nume");
            System.out.println("3. Modifică email");
            System.out.println("4. Modifică telefon");
            System.out.println("5. Modifică certificări");
            System.out.println("6. Modifică ani de experiență");
            System.out.println("7. Modifică cod intern");
            System.out.println("8. Modifică status urgențe (on call)");
            System.out.println("9. Modifică tura (DAY/NIGHT)");
            System.out.println("0. Revenire");
            System.out.print("Alegere: ");
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> {
                    System.out.print("Prenume nou: ");
                    nurse.setFirstName(scanner.nextLine());
                }
                case 2 -> {
                    System.out.print("Nume nou: ");
                    nurse.setLastName(scanner.nextLine());
                }
                case 3 -> {
                    System.out.print("Email nou: ");
                    nurse.setEmail(scanner.nextLine());
                }
                case 4 -> {
                    System.out.print("Telefon nou: ");
                    nurse.setPhoneNumber(scanner.nextLine());
                }
                case 5 -> {
                    System.out.print("Certificări noi: ");
                    nurse.setCertifications(scanner.nextLine());
                }
                case 6 -> {
                    System.out.print("Ani experiență: ");
                    nurse.setYearsOfExperience(scanner.nextInt());
                    scanner.nextLine();
                }
                case 7 -> {
                    System.out.print("Cod intern nou: ");
                    nurse.setStaffCode(scanner.nextLine());
                }
                case 8 -> {
                    System.out.print("Disponibilă la urgențe (true/false): ");
                    nurse.setOnCall(Boolean.parseBoolean(scanner.nextLine()));
                }
                case 9 -> {
                    System.out.print("Tură (DAY/NIGHT): ");
                    nurse.setShift(Shift.valueOf(scanner.nextLine().toUpperCase()));
                }
                case 0 -> System.out.println("Revenire...");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);

        updateNurseById(id, nurse);
    }
    public List<Nurse> getNursesForDoctor(int doctorId) {
        List<Nurse> nursesForDoctor = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT n.* FROM nurses n " +
                    "JOIN doctor_nurse dn ON n.id = dn.nurse_id " +
                    "WHERE dn.doctor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctorId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Nurse nurse = new Nurse(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        Shift.valueOf(rs.getString("shift")),
                        rs.getString("staff_code"),
                        rs.getString("certifications"),
                        rs.getInt("years_of_experience"),
                        rs.getBoolean("is_on_call")
                );
                nurse.setId(rs.getInt("id"));
                nursesForDoctor.add(nurse);
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare la încărcarea asistentelor pentru doctor: " + e.getMessage());
        }

        return nursesForDoctor;
    }

}
