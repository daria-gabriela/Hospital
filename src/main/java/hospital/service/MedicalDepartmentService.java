package main.java.hospital.service;

import main.java.hospital.model.*;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class MedicalDepartmentService {

    private static MedicalDepartmentService instance;

    public static synchronized MedicalDepartmentService getInstance() {
        if (instance == null) {
            instance = new MedicalDepartmentService();
        }
        return instance;
    }
    private RoomService roomService;

    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    private final List<MedicalDepartment> departments;

    private MedicalDepartmentService() {
        this.departments = new ArrayList<>();
    }
    public void initialize() {
        loadFromDatabase();
    }


    public void loadFromDatabase() {
        departments.clear();
        Map<Integer, Doctor> doctorById = new HashMap<>();
        Map<Integer, Nurse> nurseById = new HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // === 1. Încarcă departamente ===
            ResultSet rs = stmt.executeQuery("SELECT * FROM medical_departments");
            while (rs.next()) {
                MedicalDepartment department = new MedicalDepartment(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("floor"),
                        rs.getString("description")
                );
                departments.add(department);
            }

            // === 2. Încarcă doctori + asociere în departamente ===
            ResultSet drs = stmt.executeQuery("""
            SELECT dd.department_id, d.* 
            FROM department_doctors dd 
            JOIN doctors d ON dd.doctor_id = d.id
        """);
            while (drs.next()) {
                int deptId = drs.getInt("department_id");
                Optional<MedicalDepartment> deptOpt = getDepartmentById(deptId);
                if (deptOpt.isPresent()) {
                    Doctor doctor = new Doctor(
                            drs.getString("first_name"),
                            drs.getString("last_name"),
                            drs.getString("email"),
                            drs.getString("phone_number"),
                            Specialization.valueOf(drs.getString("specialization").toUpperCase()),
                            drs.getInt("years_of_experience"),
                            drs.getString("parafa_code"),
                            true
                    );
                    doctor.setId(drs.getInt("id"));
                    deptOpt.get().addDoctor(doctor);
                    doctorById.put(doctor.getId(), doctor);
                }
            }

            // === 3. Încarcă asistentele din legăturile reale (doar cele utilizate) ===
            ResultSet ddn = stmt.executeQuery("""
            SELECT ddn.department_id, ddn.doctor_id, ddn.nurse_id,
                   n.first_name, n.last_name, n.email, n.phone_number,
                   n.shift, n.staff_code, n.certifications,
                   n.years_of_experience, n.is_on_call
            FROM department_doctor_nurse ddn
            JOIN nurses n ON ddn.nurse_id = n.id
        """);

            while (ddn.next()) {
                int deptId = ddn.getInt("department_id");
                int doctorId = ddn.getInt("doctor_id");

                Nurse nurse = new Nurse(
                        ddn.getString("first_name"),
                        ddn.getString("last_name"),
                        ddn.getString("email"),
                        ddn.getString("phone_number"),
                        Shift.valueOf(ddn.getString("shift").toUpperCase()),
                        ddn.getString("staff_code"),
                        ddn.getString("certifications"),
                        ddn.getInt("years_of_experience"),
                        ddn.getBoolean("is_on_call")
                );
                nurse.setId(ddn.getInt("nurse_id"));
                nurseById.putIfAbsent(nurse.getId(), nurse);

                MedicalDepartment dept = getDepartmentById(deptId).orElse(null);
                Doctor doctor = doctorById.get(doctorId);

                if (dept != null && doctor != null) {
                    dept.addNurse(nurse);
                    dept.addNurseToDoctor(doctor, nurse);
                }
            }

            // === 4. Camere asociate departamentelor ===
            for (Room room : roomService.getAllRooms()) {
                int deptId = room.getDepartment() != null ? room.getDepartment().getId() : -1;
                getDepartmentById(deptId).ifPresent(dept -> {
                    room.setDepartment(dept);
                    dept.addRoom(room);
                });
            }

            AuditService.getInstance().log("LOAD_DEPARTMENTS_FROM_DB");

        } catch (SQLException e) {
            System.err.println("❌ Eroare la încărcarea departamentelor: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void addDepartment(MedicalDepartment department) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO medical_departments (name, floor, description) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, department.getName());
            stmt.setString(2, department.getFloor());
            stmt.setString(3, department.getDescription());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    department.setId(rs.getInt(1));
                }
                departments.add(department);
                AuditService.getInstance().log("ADD_DEPARTMENT: " + department.getName());
            }

        } catch (SQLException e) {
            System.err.println("❌ Eroare la adăugarea departamentului: " + e.getMessage());
        }
    }

    public List<MedicalDepartment> getAllDepartments() {
        AuditService.getInstance().log("READ_ALL_DEPARTMENTS");
        return new ArrayList<>(departments);
    }

    public Optional<MedicalDepartment> getDepartmentById(int id) {
        AuditService.getInstance().log("GET_DEPARTMENT_BY_ID: " + id);
        return departments.stream().filter(d -> d.getId() == id).findFirst();
    }

    public boolean deleteDepartmentById(int id) {
        Optional<MedicalDepartment> optional = getDepartmentById(id);
        if (optional.isEmpty()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM department_doctors WHERE department_id = ?");
            stmt.setInt(1, id);
            stmt.executeUpdate();

            stmt = conn.prepareStatement("DELETE FROM medical_departments WHERE id = ?");
            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                departments.remove(optional.get());
                AuditService.getInstance().log("DELETE_DEPARTMENT: ID=" + id);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la ștergerea departamentului: " + e.getMessage());
        }

        return false;
    }

    public void displayDepartmentsSorted(boolean ascending) {
        List<MedicalDepartment> sorted = departments.stream()
                .sorted(Comparator.comparing(MedicalDepartment::getName))
                .collect(Collectors.toList());
        if (!ascending) Collections.reverse(sorted);

        for (MedicalDepartment dept : sorted) {
            System.out.println(dept);
        }

        AuditService.getInstance().log("DISPLAY_DEPARTMENTS_SORTED_" + (ascending ? "AZ" : "ZA"));
    }

    public boolean updateDepartmentName(int id, String newName) {
        Optional<MedicalDepartment> optional = getDepartmentById(id);
        if (optional.isEmpty()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_departments SET name = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newName);
            stmt.setInt(2, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                optional.get().setName(newName);
                AuditService.getInstance().log("UPDATE_DEPARTMENT_NAME: ID=" + id);
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Eroare actualizare nume departament: " + e.getMessage());
        }

        return false;
    }

    public boolean updateDepartmentFloor(int id, String newFloor) {
        Optional<MedicalDepartment> optional = getDepartmentById(id);
        if (optional.isEmpty()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_departments SET floor = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newFloor);
            stmt.setInt(2, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                optional.get().setFloor(newFloor);
                AuditService.getInstance().log("UPDATE_DEPARTMENT_FLOOR: ID=" + id);
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Eroare actualizare etaj departament: " + e.getMessage());
        }

        return false;
    }

    public boolean updateDepartmentDescription(int id, String newDescription) {
        Optional<MedicalDepartment> optional = getDepartmentById(id);
        if (optional.isEmpty()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE medical_departments SET description = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newDescription);
            stmt.setInt(2, id);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                optional.get().setDescription(newDescription);
                AuditService.getInstance().log("UPDATE_DEPARTMENT_DESCRIPTION: ID=" + id);
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Eroare actualizare descriere departament: " + e.getMessage());
        }

        return false;
    }

    public boolean addDoctorToDepartment(int id, Doctor doctor) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO department_doctors (department_id, doctor_id) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.setInt(2, doctor.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Eroare la persistarea doctorului în departament: " + e.getMessage());
        }

        return getDepartmentById(id).map(d -> {
            d.addDoctor(doctor);
            AuditService.getInstance().log("ADD_DOCTOR_TO_DEPARTMENT: ID=" + id + ", Doctor=" + doctor.getFullName());
            return true;
        }).orElse(false);
    }


    public boolean addNurseToDoctor(Doctor doctor, Nurse nurse) {
        boolean added = false;

        for (MedicalDepartment dept : departments) {
            if (dept.getDoctors().contains(doctor)) {
                boolean addedToDoctor = dept.addNurseToDoctor(doctor, nurse);
                boolean addedToDepartment = dept.addNurse(nurse); // ✅ Asta lipsea

                if (addedToDoctor || addedToDepartment) {
                    added = true;
                }
            }
        }

        if (added) {
            AuditService.getInstance().log("ADD_NURSE_TO_DOCTOR_ALL_DEPARTMENTS: DoctorID=" + doctor.getId() + ", NurseID=" + nurse.getId());
        } else {
            AuditService.getInstance().log("FAILED_ADD_NURSE_TO_DOCTOR_ALL_DEPARTMENTS: DoctorID=" + doctor.getId());
        }

        return added;
    }

    public boolean removeNurseFromDoctor(int departmentId, Doctor doctor, Nurse nurse) {
        return getDepartmentById(departmentId).map(d -> {
            boolean success = d.removeNurseFromDoctor(doctor, nurse);
            AuditService.getInstance().log((success ? "REMOVE" : "FAILED_REMOVE") + "_NURSE_FROM_DOCTOR: DeptID=" + departmentId);
            return success;
        }).orElse(false);
    }

    public boolean addNurseToDoctorInDb(int doctorId, int nurseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO doctor_nurses (doctor_id, nurse_id) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, doctorId);
            stmt.setInt(2, nurseId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Eroare la asocierea nurse-doctor: " + e.getMessage());
        }
        return false;
    }

    public boolean updateRoomInDepartment(int departmentId, int roomNumber, String newType, boolean newOccupied) {
        Optional<MedicalDepartment> deptOpt = getDepartmentById(departmentId);
        if (deptOpt.isEmpty()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE rooms SET type = ?, is_occupied = ? WHERE room_number = ? AND department_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newType);
            stmt.setBoolean(2, newOccupied);
            stmt.setInt(3, roomNumber);
            stmt.setInt(4, departmentId);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                for (Room r : deptOpt.get().getRooms()) {
                    if (r.getRoomNumber() == roomNumber) {
                        r.setType(newType);
                        r.setOccupied(newOccupied);
                        break;
                    }
                }
                AuditService.getInstance().log("UPDATE_ROOM_IN_DEPARTMENT: DeptID=" + departmentId + ", Room=" + roomNumber);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare la actualizarea camerei în departament: " + e.getMessage());
        }

        return false;
    }

    public boolean addRoomToDepartment(int departmentId, Room room) {
        Optional<MedicalDepartment> deptOpt = getDepartmentById(departmentId);
        if (deptOpt.isEmpty()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO rooms (room_number, type, is_occupied, department_id) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, room.getRoomNumber());
            stmt.setString(2, room.getType());
            stmt.setBoolean(3, room.isOccupied());
            stmt.setInt(4, departmentId);
            stmt.executeUpdate();

            deptOpt.get().addRoom(room);
            AuditService.getInstance().log("ADD_ROOM_TO_DEPARTMENT: DeptID=" + departmentId + ", Room=" + room.getRoomNumber());
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Eroare la adăugarea camerei în departament: " + e.getMessage());
            return false;
        }
    }

    public boolean removeRoomFromDepartment(int departmentId, int roomNumber) {
        Optional<MedicalDepartment> deptOpt = getDepartmentById(departmentId);
        if (deptOpt.isEmpty()) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM rooms WHERE room_number = ? AND department_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, roomNumber);
            stmt.setInt(2, departmentId);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                deptOpt.get().getRooms().removeIf(r -> r.getRoomNumber() == roomNumber);
                AuditService.getInstance().log("REMOVE_ROOM_FROM_DEPARTMENT: DeptID=" + departmentId + ", Room=" + roomNumber);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare la ștergerea camerei din departament: " + e.getMessage());
        }

        return false;
    }

    public List<Room> getRoomsByDepartmentId(int departmentId) {
        return getDepartmentById(departmentId)
                .map(MedicalDepartment::getRooms)
                .orElse(Collections.emptyList());
    }

    public boolean removeDoctorFromDepartment(int id, Doctor doctor) {
        Optional<MedicalDepartment> optional = getDepartmentById(id);
        if (optional.isEmpty()) return false;

        MedicalDepartment department = optional.get();
        if (!department.getDoctors().contains(doctor)) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM department_doctors WHERE department_id = ? AND doctor_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.setInt(2, doctor.getId());
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                department.removeDoctor(doctor);
                AuditService.getInstance().log("REMOVE_DOCTOR_FROM_DEPARTMENT: ID=" + id + ", Doctor=" + doctor.getFullName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare la ștergerea doctorului din departament: " + e.getMessage());
        }

        return false;
    }


    public void loadDepartmentsOnly() {
            departments.clear();
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SELECT * FROM medical_departments");
                while (rs.next()) {
                    MedicalDepartment department = new MedicalDepartment(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("floor"),
                            rs.getString("description")
                    );
                    departments.add(department);
                }

                AuditService.getInstance().log("LOAD_ONLY_DEPARTMENTS");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    public void refreshRooms() {
        if (roomService != null) {
            roomService.loadRoomsFromDB();
            for (MedicalDepartment dept : departments) {
                dept.getRooms().clear(); // curăță camerele vechi
            }

            for (Room room : roomService.getAllRooms()) {
                int deptId = room.getDepartment() != null ? room.getDepartment().getId() : -1;
                getDepartmentById(deptId).ifPresent(dept -> {
                    room.setDepartment(dept);
                    dept.addRoom(room);
                });
            }

            AuditService.getInstance().log("REFRESH_ROOMS_AND_DEPARTMENTS");
        }
    }


    public boolean addNurseToDoctorInSpecificDepartment(int departmentId, Doctor doctor, Nurse nurse) {
        Optional<MedicalDepartment> optional = getDepartmentById(departmentId);
        if (optional.isEmpty()) return false;

        MedicalDepartment dept = optional.get();
        if (!dept.getDoctors().contains(doctor)) return false;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String check = "SELECT 1 FROM department_doctor_nurse WHERE department_id = ? AND doctor_id = ? AND nurse_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(check);
            checkStmt.setInt(1, departmentId);
            checkStmt.setInt(2, doctor.getId());
            checkStmt.setInt(3, nurse.getId());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("⚠️ Această asociere există deja.");
                return false;
            }

            String insert = "INSERT INTO department_doctor_nurse (department_id, doctor_id, nurse_id) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insert);
            insertStmt.setInt(1, departmentId);
            insertStmt.setInt(2, doctor.getId());
            insertStmt.setInt(3, nurse.getId());
            insertStmt.executeUpdate();

            dept.addNurse(nurse); // în lista generală
            dept.addNurseToDoctor(doctor, nurse); // legat direct

            AuditService.getInstance().log("ADD_NURSE_TO_DOCTOR_IN_DEPARTMENT: DeptID=" + departmentId);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Eroare la asocierea nurse-doctor-departament: " + e.getMessage());
        }

        return false;
    }
    public boolean addNurseToDoctorInAllDepartments(Doctor doctor, Nurse nurse) {
        boolean success = false;

        for (MedicalDepartment dept : departments) {
            if (dept.getDoctors().contains(doctor)) {
                boolean added = dept.addNurseToDoctor(doctor, nurse);
                if (added) {
                    dept.addNurse(nurse);
                    insertDoctorNurseRelation(dept.getId(), doctor.getId(), nurse.getId());
                    success = true;
                }
            }
        }

        if (success) {
            AuditService.getInstance().log("ADD_NURSE_TO_DOCTOR_ALL_DEPARTMENTS");
        }

        return success;
    }
    private void insertDoctorNurseRelation(int departmentId, int doctorId, int nurseId) {
        String sql = "INSERT INTO department_doctor_nurse (department_id, doctor_id, nurse_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, departmentId);
            stmt.setInt(2, doctorId);
            stmt.setInt(3, nurseId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Eroare la inserarea relației doctor-asistentă-departament: " + e.getMessage());
        }
    }

}