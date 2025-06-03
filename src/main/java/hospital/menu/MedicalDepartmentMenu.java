package main.java.hospital.menu;

import main.java.hospital.model.*;
import main.java.hospital.service.*;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MedicalDepartmentMenu {
    private final MedicalDepartmentService departmentService;
    private final RoomService roomService;
    private final NurseService nurseService;
    private final DoctorService doctorService = new DoctorService();
    private final Scanner scanner = new Scanner(System.in);

    public MedicalDepartmentMenu(MedicalDepartmentService departmentService, RoomService roomService, NurseService nurseService) {
        this.departmentService = departmentService;
        this.roomService = roomService;
        this.nurseService = nurseService;
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU DEPARTAMENTE ===");
            System.out.println("1. Adaugă departament");
            System.out.println("2. Șterge departament");
            System.out.println("3. Afișează toate departamentele (A-Z sau Z-A)");
            System.out.println("4. Meniu camere");
            System.out.println("5. Editează un departament");
            System.out.println("6. Adaugă doctor în departament");
            System.out.println("7. Adaugă asistentă unui doctor");
            System.out.println("8. Elimină doctor din departament");
            System.out.println("9. Elimină asistentă din doctor");
            System.out.println("0. Înapoi");
            System.out.print("Alegere: ");
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> addDepartment();
                case 2 -> deleteDepartment();
                case 3 -> displaySortedDepartments();
                case 4 -> showRoomMenuForSelectedDepartment();
                case 5 -> editDepartmentMenu();
                case 6 -> addDoctorToDepartment();
                case 7 -> addNurseToDoctorInDepartment();
                case 8 -> removeDoctorFromDepartment();
                case 9 -> removeNurseFromDoctor();
                case 0 -> System.out.println("Revenire...");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void showRoomMenuForSelectedDepartment() {
        departmentService.displayDepartmentsSorted(true);
        System.out.print("ID departament pentru camere: ");
        int deptId = scanner.nextInt();
        scanner.nextLine();
        Optional<MedicalDepartment> optional = departmentService.getDepartmentById(deptId);
        if (optional.isPresent()) {
            RoomMenu menu = new RoomMenu(deptId);
            menu.showMenu();
        } else {
            System.out.println("❌ Departament inexistent.");
        }
    }

    private void addDepartment() {
        System.out.print("Nume: ");
        String name = scanner.nextLine();
        System.out.print("Etaj: ");
        String floor = scanner.nextLine();
        System.out.print("Descriere: ");
        String desc = scanner.nextLine();

        MedicalDepartment dept = new MedicalDepartment(name, floor, desc);
        departmentService.addDepartment(dept);
        System.out.println("✅ Departament adăugat.");
    }

    private void deleteDepartment() {
        System.out.print("ID: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        if (departmentService.deleteDepartmentById(id)) {
            System.out.println("🗑️ Departament șters.");
        } else {
            System.out.println("❌ Nu s-a găsit departamentul.");
        }
    }

    private void displaySortedDepartments() {
        System.out.print("Sortare A-Z (1) sau Z-A (2)? ");
        int sortChoice = scanner.nextInt();
        scanner.nextLine();
        departmentService.displayDepartmentsSorted(sortChoice == 1);
    }

    private void editDepartmentMenu() {
        System.out.print("ID departament: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        System.out.println("1. Nume nou");
        System.out.println("2. Etaj nou");
        System.out.println("3. Descriere nouă");
        System.out.print("Alegere: ");
        int field = scanner.nextInt();
        scanner.nextLine();

        switch (field) {
            case 1 -> {
                System.out.print("Nume nou: ");
                String name = scanner.nextLine();
                departmentService.updateDepartmentName(id, name);
            }
            case 2 -> {
                System.out.print("Etaj nou: ");
                String floor = scanner.nextLine();
                departmentService.updateDepartmentFloor(id, floor);
            }
            case 3 -> {
                System.out.print("Descriere nouă: ");
                String desc = scanner.nextLine();
                departmentService.updateDepartmentDescription(id, desc);
            }
            default -> System.out.println("❌ Opțiune invalidă.");
        }
    }

    private void addDoctorToDepartment() {
        departmentService.displayDepartmentsSorted(true);
        System.out.print("ID departament: ");
        int deptId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Vrei să adaugi un doctor existent (E) sau să creezi unul nou (N)? ");
        String choice = scanner.nextLine();

        Doctor doctor;

        if (choice.equalsIgnoreCase("E")) {
            List<Doctor> doctors = doctorService.getAllDoctors();
            if (doctors.isEmpty()) {
                System.out.println("⚠️ Nu există doctori înregistrați.");
                return;
            }
            for (int i = 0; i < doctors.size(); i++) {
                System.out.println((i + 1) + ". " + doctors.get(i).getFullName() + " (" + doctors.get(i).getParafaCode() + ")");
            }
            System.out.print("Alege doctor (număr): ");
            int index = scanner.nextInt();
            scanner.nextLine();
            if (index < 1 || index > doctors.size()) {
                System.out.println("❌ Opțiune invalidă.");
                return;
            }
            doctor = doctors.get(index - 1);
        } else {
            System.out.print("Prenume doctor: ");
            String firstName = scanner.nextLine();
            System.out.print("Nume doctor: ");
            String lastName = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Telefon: ");
            String phone = scanner.nextLine();
            System.out.print("Specializare (ex: CARDIOLOGIE): ");
            String specializationInput = scanner.nextLine();
            System.out.print("Ani experiență: ");
            int years = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Cod parafă: ");
            String code = scanner.nextLine();

            try {
                Specialization specialization = Specialization.valueOf(specializationInput.toUpperCase());
                doctor = doctorService.addDoctor(firstName, lastName, email, phone, specialization, years, code);
            } catch (IllegalArgumentException e) {
                System.out.println("❌ Specializare invalidă.");
                return;
            }
        }

        departmentService.addDoctorToDepartment(deptId, doctor);
        System.out.println("✅ Doctor adăugat în departament.");
    }

    private void removeDoctorFromDepartment() {
        MedicalDepartment dept = selectDepartment();
        if (dept == null) return;

        Doctor doctor = selectDoctorFrom(dept);
        if (doctor == null) return;

        boolean removed = departmentService.removeDoctorFromDepartment(dept.getId(), doctor);
        if (removed) {
            System.out.println("✅ Doctor eliminat din departament.");
        } else {
            System.out.println("❌ Nu s-a putut elimina doctorul.");
        }
    }

    private void removeNurseFromDoctor() {
        MedicalDepartment dept = selectDepartment();
        if (dept == null) return;

        Doctor doctor = selectDoctorFrom(dept);
        if (doctor == null) return;

        List<Nurse> assigned = dept.getNursesForDoctor(doctor);
        if (assigned.isEmpty()) {
            System.out.println("❌ Doctorul nu are asistente asociate.");
            return;
        }

        for (int i = 0; i < assigned.size(); i++) {
            System.out.println((i + 1) + ". " + assigned.get(i).getFullName());
        }
        System.out.print("Alege asistentă de eliminat (număr): ");
        int index = scanner.nextInt();
        scanner.nextLine();

        if (index >= 1 && index <= assigned.size()) {
            Nurse nurse = assigned.get(index - 1);
            boolean removed = departmentService.removeNurseFromDoctor(dept.getId(), doctor, nurse);
            if (removed) {
                System.out.println("✅ Asistentă eliminată din doctor.");
            } else {
                System.out.println("❌ Eroare la eliminare asistentă.");
            }
        } else {
            System.out.println("❌ Opțiune invalidă.");
        }
    }

    private void addNurseToDoctorInDepartment() {
        MedicalDepartment dept = selectDepartment();
        if (dept == null) return;

        Doctor doctor = selectDoctorFrom(dept);
        if (doctor == null) return;

        System.out.print("Vrei să adaugi o asistentă existentă (E) sau să creezi una nouă (N)? ");
        String choice = scanner.nextLine();

        Nurse nurse;
        if (choice.equalsIgnoreCase("E")) {
            List<Nurse> nurses = nurseService.getAllNurses();
            if (nurses.isEmpty()) {
                System.out.println("⚠️ Nu există asistente.");
                return;
            }
            for (int i = 0; i < nurses.size(); i++) {
                System.out.println((i + 1) + ". " + nurses.get(i).getFullName());
            }
            System.out.print("Alege asistentă (număr): ");
            int index = scanner.nextInt();
            scanner.nextLine();
            if (index < 1 || index > nurses.size()) {
                System.out.println("❌ Opțiune invalidă.");
                return;
            }
            nurse = nurses.get(index - 1);
        } else {
            nurse = new NurseMenu(nurseService).createNurseFromInput();
        }

        // 🔁 Asociază în toate departamentele unde există doctorul
        boolean success = departmentService.addNurseToDoctorInAllDepartments(doctor, nurse);

        if (success) {
            System.out.println("✅ Asistentă adăugată doctorului în toate departamentele sale.");
        } else {
            System.out.println("❌ Eroare la asociere (deja există sau invalid).");
        }
    }



    private MedicalDepartment selectDepartment() {
        departmentService.displayDepartmentsSorted(true);
        System.out.print("ID departament: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        return departmentService.getDepartmentById(id).orElse(null);
    }

    private Doctor selectDoctorFrom(MedicalDepartment dept) {
        List<Doctor> doctors = dept.getDoctors();
        if (doctors.isEmpty()) {
            System.out.println("⚠️ Nu există doctori.");
            return null;
        }
        for (int i = 0; i < doctors.size(); i++) {
            System.out.println((i + 1) + ". " + doctors.get(i).getFullName());
        }
        System.out.print("Alege doctor (număr): ");
        int idx = scanner.nextInt() - 1;
        scanner.nextLine();
        return (idx >= 0 && idx < doctors.size()) ? doctors.get(idx) : null;
    }
}
