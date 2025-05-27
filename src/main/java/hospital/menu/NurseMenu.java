package main.java.hospital.menu;

import main.java.hospital.model.Nurse;
import main.java.hospital.model.Shift;
import main.java.hospital.service.NurseService;
import main.java.hospital.util.AuditService;

import java.util.List;
import java.util.Scanner;

public class NurseMenu {
    private final NurseService nurseService;
    private final Scanner scanner;

    public NurseMenu(NurseService nurseService) {
        this.nurseService = nurseService;
        this.scanner = new Scanner(System.in);
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU ASISTENTE MEDICALE ===");
            System.out.println("1. Adaugă asistentă");
            System.out.println("2. Șterge asistentă după ID");
            System.out.println("3. Editează atributele unei asistente");
            System.out.println("4. Afișează toate asistentele");
            System.out.println("5. Caută după nume");
            System.out.println("6. Filtrează după tură");
            System.out.println("7. Afișează asistente de gardă");
            System.out.println("8. Caută după certificare");
            System.out.println("9. Caută după vechime minimă");
            System.out.println("0. Înapoi");
            System.out.print("Alegere: ");
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> addNurse();
                case 2 -> removeNurse();
                case 3 -> editNurse();
                case 4 -> displayAll();
                case 5 -> searchByName();
                case 6 -> filterByShift();
                case 7 -> displayOnCall();
                case 8 -> searchByCert();
                case 9 -> filterByExperience();
                case 0 -> System.out.println("↩️ Revenire la meniul anterior.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void addNurse() {
        try {
            Nurse nurse = createNurseFromInput();
            nurseService.addNurse(nurse);
            System.out.println("✅ Asistentă adăugată cu succes.");
            AuditService.getInstance().log("CREATE_NURSE: " + nurse.getFullName());
        } catch (Exception e) {
            System.out.println("❌ Eroare la adăugare: " + e.getMessage());
        }
    }

    private void removeNurse() {
        System.out.print("🔎 ID asistentă: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        if (nurseService.removeNurseById(id)) {
            System.out.println("🗑️ Asistentă ștearsă.");
            AuditService.getInstance().log("DELETE_NURSE: ID " + id);
        } else {
            System.out.println("❌ Asistentă inexistentă.");
        }
    }

    private void editNurse() {
        System.out.print("✏️ ID asistentă de editat: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        nurseService.editNurseById(id, scanner);
        AuditService.getInstance().log("EDIT_NURSE: ID " + id);
    }

    private void displayAll() {
        List<Nurse> list = nurseService.getAllNurses();
        if (list.isEmpty()) {
            System.out.println("📭 Nu există asistente înregistrate.");
        } else {
            list.forEach(System.out::println);
        }
    }

    private void searchByName() {
        System.out.print("🔍 Nume căutat: ");
        String name = scanner.nextLine();
        var results = nurseService.searchNursesByName(name);
        if (results.isEmpty()) {
            System.out.println("❌ Nicio asistentă găsită.");
        } else {
            results.forEach(System.out::println);
        }
    }

    private void filterByShift() {
        try {
            System.out.print("Tura (DAY/NIGHT): ");
            Shift shift = Shift.valueOf(scanner.nextLine().toUpperCase());
            var results = nurseService.getNursesByShift(shift);
            if (results.isEmpty()) {
                System.out.println("❌ Nicio asistentă pe această tură.");
            } else {
                results.forEach(System.out::println);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("⚠️ Tura introdusă este invalidă.");
        }
    }

    private void displayOnCall() {
        var results = nurseService.getOnCallNurses();
        if (results.isEmpty()) {
            System.out.println("❌ Nicio asistentă de gardă.");
        } else {
            results.forEach(System.out::println);
        }
    }

    private void searchByCert() {
        System.out.print("Certificare (ex: BLS): ");
        String cert = scanner.nextLine();
        var results = nurseService.getNursesWithCertification(cert);
        if (results.isEmpty()) {
            System.out.println("❌ Nicio potrivire găsită.");
        } else {
            results.forEach(System.out::println);
        }
    }

    private void filterByExperience() {
        System.out.print("🔢 Minim ani experiență: ");
        int min = scanner.nextInt();
        scanner.nextLine();
        var results = nurseService.getExperiencedNurses(min);
        if (results.isEmpty()) {
            System.out.println("❌ Nicio asistentă cu suficientă experiență.");
        } else {
            results.forEach(System.out::println);
        }
    }

    public Nurse createNurseFromInput() {
        System.out.print("Prenume: ");
        String firstname = scanner.nextLine();
        System.out.print("Nume: ");
        String lastname = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Telefon: ");
        String phone = scanner.nextLine();
        System.out.print("Cod personal (staff code): ");
        String code = scanner.nextLine();

        Shift shift;
        while (true) {
            try {
                System.out.print("Tura (DAY/NIGHT): ");
                shift = Shift.valueOf(scanner.nextLine().toUpperCase());
                break;
            } catch (Exception e) {
                System.out.println("❌ Tura invalidă. Reintrodu.");
            }
        }

        System.out.print("Certificări (ex: BLS, ACLS): ");
        String certs = scanner.nextLine();

        System.out.print("Ani experiență: ");
        int years = scanner.nextInt();
        scanner.nextLine();

        boolean onCall;
        while (true) {
            System.out.print("Este de gardă? (true/false): ");
            String input = scanner.nextLine().toLowerCase();
            if (input.equals("true") || input.equals("false")) {
                onCall = Boolean.parseBoolean(input);
                break;
            } else {
                System.out.println("❌ Valoare invalidă. Introdu true sau false.");
            }
        }

        return new Nurse(firstname, lastname, email, phone, shift, code, certs, years, onCall);
    }
}
