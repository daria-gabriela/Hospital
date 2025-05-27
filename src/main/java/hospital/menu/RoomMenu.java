package main.java.hospital.menu;

import main.java.hospital.model.Room;
import main.java.hospital.model.MedicalDepartment;
import main.java.hospital.service.MedicalDepartmentService;
import main.java.hospital.util.AuditService;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class RoomMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final MedicalDepartmentService departmentService = MedicalDepartmentService.getInstance();
    private final AuditService audit = AuditService.getInstance();
    private final int departmentId;

    public RoomMenu(int departmentId) {
        this.departmentId = departmentId;
    }

    public void showMenu() {
        int option = 0;
        do {
            System.out.println("\n=== MENIU CAMERE ===");
            System.out.println("1. Afișează camere");
            System.out.println("2. Adaugă cameră");
            System.out.println("3. Șterge cameră");
            System.out.println("4. Modifică cameră");
            System.out.println("0. Înapoi");
            System.out.print("Alegere: ");

            try {
                option = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Introdu un număr valid.");
                continue;
            }

            switch (option) {
                case 1 -> listRooms();
                case 2 -> addRoom();
                case 3 -> deleteRoom();
                case 4 -> updateRoom();
                case 0 -> System.out.println("↩️ Revenire la meniul anterior.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void listRooms() {
        List<Room> rooms = departmentService.getRoomsByDepartmentId(departmentId);
        if (rooms.isEmpty()) {
            System.out.println("📭 Nicio cameră înregistrată.");
        } else {
            System.out.println("📋 Camere în departament:");
            for (Room room : rooms) {
                System.out.printf("🛏️ Camera %d - %s - %s\n",
                        room.getRoomNumber(), room.getType(), room.isOccupied() ? "Ocupată" : "Liberă");
            }
        }
        audit.log("LIST_ROOMS_FOR_DEPARTMENT: ID=" + departmentId);
    }

    private void addRoom() {
        try {
            Optional<MedicalDepartment> deptOpt = departmentService.getDepartmentById(departmentId);
            if (deptOpt.isEmpty()) {
                System.out.println("❌ Departament inexistent.");
                return;
            }

            int number = departmentService.getRoomsByDepartmentId(departmentId).size() + 1;
            System.out.println("🔢 Număr cameră atribuit automat: " + number);
            System.out.print("Tip cameră (ex: terapie, consultații): ");
            String type = scanner.nextLine();

            boolean occupied;
            while (true) {
                System.out.print("Este ocupată? (true/false): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")) {
                    occupied = Boolean.parseBoolean(input);
                    break;
                } else {
                    System.out.println("⚠️ Introdu true sau false.");
                }
            }

            Room room = new Room(number, type, deptOpt.get(), occupied);
            boolean success = departmentService.addRoomToDepartment(departmentId, room);

            if (success) {
                System.out.println("✅ Cameră adăugată cu succes.");
                audit.log("ADD_ROOM: deptId=" + departmentId + ", nr=" + number);
            } else {
                System.out.println("❌ Eroare la adăugare cameră.");
            }
        } catch (Exception e) {
            System.out.println("❌ Eroare: " + e.getMessage());
        }
    }

    private void deleteRoom() {
        System.out.print("🔢 Număr cameră de șters: ");
        try {
            int number = Integer.parseInt(scanner.nextLine());
            boolean success = departmentService.removeRoomFromDepartment(departmentId, number);
            if (success) {
                System.out.println("✅ Cameră ștearsă.");
                audit.log("DELETE_ROOM: deptId=" + departmentId + ", nr=" + number);
            } else {
                System.out.println("❌ Cameră inexistentă.");
            }
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Număr invalid.");
        }
    }

    private void updateRoom() {
        try {
            System.out.print("🔢 Număr cameră de modificat: ");
            int number = Integer.parseInt(scanner.nextLine());

            System.out.print("🔧 Nou tip cameră: ");
            String newType = scanner.nextLine();

            boolean occupied;
            while (true) {
                System.out.print("Este ocupată? (true/false): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")) {
                    occupied = Boolean.parseBoolean(input);
                    break;
                } else {
                    System.out.println("⚠️ Introdu true sau false.");
                }
            }

            boolean success = departmentService.updateRoomInDepartment(departmentId, number, newType, occupied);
            if (success) {
                System.out.println("✅ Cameră actualizată.");
                audit.log("UPDATE_ROOM: deptId=" + departmentId + ", nr=" + number);
            } else {
                System.out.println("❌ Cameră inexistentă sau eroare la actualizare.");
            }

        } catch (NumberFormatException e) {
            System.out.println("⚠️ Număr invalid.");
        } catch (Exception e) {
            System.out.println("❌ Eroare: " + e.getMessage());
        }
    }
}
