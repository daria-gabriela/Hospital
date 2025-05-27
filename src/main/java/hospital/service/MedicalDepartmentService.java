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

            ResultSet drs = stmt.executeQuery("SELECT dd.department_id, d.* FROM department_doctors dd JOIN doctors d ON dd.doctor_id = d.id");
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

            ResultSet nrs = stmt.executeQuery("""
                SELECT DISTINCT dn.nurse_id, n.*
                FROM doctor_nurses dn
                JOIN nurses n ON dn.nurse_id = n.id
            """);

            while (nrs.next()) {
                Nurse nurse = new Nurse(
                        nrs.getString("first_name"),
                        nrs.getString("last_name"),
                        nrs.getString("email"),
                        nrs.getString("phone_number"),
                        Shift.valueOf(nrs.getString("shift").toUpperCase()),
                        nrs.getString("staff_code"),
                        nrs.getString("certifications"),
                        nrs.getInt("years_of_experience"),
                        nrs.getBoolean("is_on_call")
                );
                nurse.setId(nrs.getInt("nurse_id"));
                nurseById.put(nurse.getId(), nurse);
            }

            ResultSet docNurseRs = stmt.executeQuery("SELECT * FROM doctor_nurses");
            while (docNurseRs.next()) {
                int doctorId = docNurseRs.getInt("doctor_id");
                int nurseId = docNurseRs.getInt("nurse_id");

                Doctor doctor = doctorById.get(doctorId);
                Nurse nurse = nurseById.get(nurseId);

                if (doctor != null && nurse != null) {
                    for (MedicalDepartment dept : departments) {
                        if (dept.getDoctors().contains(doctor)) {
                            dept.addNurse(nurse);
                            dept.addNurseToDoctor(doctor, nurse);
                        }
                    }
                }
            }

            for (Room room : roomService.getAllRooms()) {
                int deptId = room.getDepartment() != null ? room.getDepartment().getId() : -1;
                getDepartmentById(deptId).ifPresent(dept -> {
                    room.setDepartment(dept); // 🔄 sincronizează referința
                    dept.addRoom(room);
                });
            }


            AuditService.getInstance().log("LOAD_DEPARTMENTS_FROM_DB");

        } catch (SQLException e) {
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

    public boolean addNurseToDoctor(int departmentId, Doctor doctor, Nurse nurse) {
        return getDepartmentById(departmentId).map(d -> {
            boolean success = d.addNurseToDoctor(doctor, nurse);
            AuditService.getInstance().log((success ? "ADD" : "FAILED_ADD") + "_NURSE_TO_DOCTOR: DeptID=" + departmentId);
            return success;
        }).orElse(false);
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

}