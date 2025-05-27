
package main.java.hospital.service;

import main.java.hospital.model.*;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomService {


    private final List<Room> allRooms = new ArrayList<>();
    private final AuditService audit = AuditService.getInstance();
    private MedicalDepartmentService departmentService;

    public RoomService() {
        // Nu încărcăm camerele aici pentru a evita recursivitate
    }

    public void setDepartmentService(MedicalDepartmentService departmentService) {
        this.departmentService = departmentService;
        loadRoomsFromDB();
    }

    public void loadRoomsFromDB() {
        allRooms.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM rooms";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int number = rs.getInt("room_number");
                String type = rs.getString("type");
                boolean occupied = rs.getBoolean("is_occupied");
                int deptId = rs.getInt("department_id");

                Optional<MedicalDepartment> deptOpt = departmentService.getDepartmentById(deptId);
                if (deptOpt.isPresent()) {
                    MedicalDepartment realDept = deptOpt.get();  // <- instanța corectă din listă
                    Room room = new Room(number, type, realDept, occupied);
                    room.setOccupied(occupied);
                    allRooms.add(room);
                    realDept.addRoom(room);  // adaugi în lista corectă
                }

            }

            audit.log("LOAD_ROOMS_FROM_DB");

        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare camere: " + e.getMessage());
        }
    }

    public void addRoom(Room room) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO rooms (type, is_occupied, department_id) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, room.getType());
            stmt.setBoolean(2, room.isOccupied());
            stmt.setInt(3, room.getDepartment().getId());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                room.setRoomNumber(generatedId); // actualizezi ID-ul în obiectul Room
            }

            allRooms.add(room);
            room.getDepartment().addRoom(room);

            audit.log("Adăugare cameră număr: " + room.getRoomNumber() +
                    " în departamentul: " + room.getDepartment().getName());

        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la adăugare cameră: " + e.getMessage());
        }
    }

    public boolean deleteRoom(int roomNumber) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM rooms WHERE room_number = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, roomNumber);

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                Optional<Room> roomOpt = getRoomByNumber(roomNumber);

                roomOpt.ifPresent(r -> r.getDepartment().removeRoom(roomNumber));
                allRooms.removeIf(r -> r.getRoomNumber() == roomNumber);
                audit.log("Ștergere cameră număr: " + roomNumber);

                if (departmentService != null) {
                    departmentService.refreshRooms();
                }

                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la ștergere cameră: " + e.getMessage());
        }

        audit.log("Eroare ștergere cameră: cameră inexistentă cu numărul: " + roomNumber);
        return false;
    }

    public Optional<Room> getRoomByNumber(int roomNumber) {
        Optional<Room> room = allRooms.stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .findFirst();

        audit.log("Căutare cameră după număr: " + roomNumber +
                " => " + (room.isPresent() ? "găsită" : "negăsită"));

        return room;
    }

    public List<Room> getAllRooms() {
        audit.log("Accesare listă toate camerele");
        return new ArrayList<>(allRooms);
    }

    public void displayAllRooms() {
        audit.log("Afișare toate camerele din sistem");
        if (allRooms.isEmpty()) {
            System.out.println("Nu există camere înregistrate.");
        } else {
            allRooms.forEach(System.out::println);
        }
    }

    public boolean editRoom(int roomNumber, String newType, Boolean isOccupied, MedicalDepartment newDepartment) {
        Optional<Room> optionalRoom = getRoomByNumber(roomNumber);
        if (optionalRoom.isEmpty()) {
            audit.log("Eroare editare cameră: nu s-a găsit camera cu numărul: " + roomNumber);
            return false;
        }

        Room room = optionalRoom.get();
        StringBuilder editInfo = new StringBuilder("Editare cameră număr: " + roomNumber + " [");

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE rooms SET type = ?, is_occupied = ?, department_id = ? WHERE room_number = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newType != null ? newType : room.getType());
            stmt.setBoolean(2, isOccupied != null ? isOccupied : room.isOccupied());
            stmt.setInt(3, newDepartment != null ? newDepartment.getId() : room.getDepartment().getId());
            stmt.setInt(4, roomNumber);
            stmt.executeUpdate();

            if (newType != null) {
                room.setType(newType);
                editInfo.append("tip=").append(newType).append(", ");
            }

            if (isOccupied != null) {
                room.setOccupied(isOccupied);
                editInfo.append("ocupată=").append(isOccupied).append(", ");
            }

            if (newDepartment != null && room.getDepartment().getId() != newDepartment.getId()) {
                room.getDepartment().removeRoom(roomNumber);
                newDepartment.addRoom(room);
                room.setDepartment(newDepartment);
                editInfo.append("departament=").append(newDepartment.getName()).append(", ");
            }

            audit.log(editInfo.toString().replaceAll(", $", "") + "]");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la actualizare cameră: " + e.getMessage());
            return false;
        }
    }

    public Room findAvailableRoom(int departmentId, String type) {
        for (Room room : allRooms) {
            if (!room.isOccupied()
                    && room.getDepartment().getId() == departmentId
                    && room.getType().equalsIgnoreCase(type)) {

                audit.log("Găsire cameră disponibilă de tip '" + type +
                        "' în departamentul ID " + departmentId +
                        ": găsită camera " + room.getRoomNumber());

                return room;
            }
        }

        audit.log("Găsire cameră disponibilă de tip '" + type +
                "' în departamentul ID " + departmentId + ": niciuna găsită");
        return null;
    }
}
