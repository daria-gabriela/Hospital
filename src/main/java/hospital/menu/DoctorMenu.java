package main.java.hospital.menu;

import main.java.hospital.model.Doctor;
import main.java.hospital.model.Specialization;
import main.java.hospital.service.DoctorService;
import main.java.hospital.util.AuditService;

import java.util.*;

public class DoctorMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final DoctorService doctorService;

    public DoctorMenu(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU DOCTORI ===");
            System.out.println("1. Adaugă doctor");
            System.out.println("2. Afișează toți doctorii");
            System.out.println("3. Caută doctor după cod parafă");
            System.out.println("4. Actualizează specializare și experiență");
            System.out.println("5. Schimbă cod parafă");
            System.out.println("6. Șterge doctor");
            System.out.println("7. Afișează doctorii grupați pe specializare");
            System.out.println("8. Afișează doctorii ordonați după experiență");
            System.out.println("9. Actualizează informații personale");
            System.out.println("0. Înapoi");
            System.out.print("Opțiune: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> addDoctor();
                case 2 -> doctorService.displayAllDoctors();
                case 3 -> findDoctorByParafaCode();
                case 4 -> updateSpecializationAndExperience();
                case 5 -> changeParafaCode();
                case 6 -> deleteDoctor();
                case 7 -> displayGroupedBySpecialization();
                case 8 -> displaySortedByExperience();
                case 9 -> updatePersonalInfo();
                case 0 -> System.out.println("Revenire la meniul principal.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void addDoctor() {
        while (true) {
            try {
                System.out.print("Prenume: ");
                String firstName = scanner.nextLine();

                System.out.print("Nume: ");
                String lastName = scanner.nextLine();

                String email;
                do {
                    System.out.print("Email: ");
                    email = scanner.nextLine();
                    if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                        System.out.println("Email invalid. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        if (email.equalsIgnoreCase("exit")) return;
                    } else break;
                } while (true);

                String phone;
                do {
                    System.out.print("Telefon (10-15 cifre, poate începe cu +): ");
                    phone = scanner.nextLine();
                    if (!phone.matches("^[0-9+]{10,15}$")) {
                        System.out.println("Număr invalid. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        if (phone.equalsIgnoreCase("exit")) return;
                    } else break;
                } while (true);

                Specialization specialization;
                while (true) {
                    System.out.println("Specializări disponibile:");
                    for (Specialization s : Specialization.values()) {
                        System.out.println("- " + s.name());
                    }
                    System.out.print("Specializare: ");
                    String input = scanner.nextLine();
                    try {
                        specialization = Specialization.valueOf(input.toUpperCase());
                        break;
                    } catch (Exception e) {
                        System.out.println("Specializare invalidă. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        if (input.equalsIgnoreCase("exit")) return;
                    }
                }

                int years;
                while (true) {
                    System.out.print("Ani de experiență: ");
                    String input = scanner.nextLine();
                    try {
                        years = Integer.parseInt(input);
                        if (years < 0) throw new NumberFormatException();
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("Ani invalidi. Introdu un număr valid sau tastează 'exit' pentru a anula.");
                        if (input.equalsIgnoreCase("exit")) return;
                    }
                }

                String code;
                do {
                    System.out.print("Cod parafă: ");
                    code = scanner.nextLine();
                    if (!code.matches("^[A-Z0-9]{3,10}$")) {
                        System.out.println("Cod parafă invalid. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        if (code.equalsIgnoreCase("exit")) return;
                    } else if (doctorService.getDoctorByParafaCode(code).isPresent()) {
                        System.out.println("Cod parafă deja folosit. Alege altul sau tastează 'exit' pentru a anula.");
                        if (code.equalsIgnoreCase("exit")) return;
                    } else break;
                } while (true);

                doctorService.addDoctor(firstName, lastName, email, phone, specialization, years, code);
                AuditService.getInstance().log("CREATE_DOCTOR: " + code);
                System.out.println("✅ Doctor adăugat cu succes!");
                break;

            } catch (Exception e) {
                System.out.println("❌ Eroare critică: " + e.getMessage());
                break;
            }
        }
    }

    private void findDoctorByParafaCode() {
        System.out.print("Cod parafă: ");
        String code = scanner.nextLine();
        doctorService.getDoctorByParafaCode(code)
                .ifPresentOrElse(System.out::println, () -> System.out.println("❌ Doctorul nu a fost găsit."));
        AuditService.getInstance().log("READ_DOCTOR: " + code);
    }

    private void updateSpecializationAndExperience() {
        System.out.print("Cod parafă: ");
        String code = scanner.nextLine();

        if (doctorService.getDoctorByParafaCode(code).isEmpty()) {
            System.out.println("❌ Doctorul nu există.");
            return;
        }

        Specialization specialization;
        while (true) {
            System.out.println("Specializări disponibile:");
            for (Specialization s : Specialization.values()) {
                System.out.println("- " + s.name());
            }
            System.out.print("Specializare: ");
            String input = scanner.nextLine();
            try {
                specialization = Specialization.valueOf(input.toUpperCase());
                break;
            } catch (Exception e) {
                System.out.println("Specializare invalidă. Încearcă din nou sau tastează 'exit' pentru a anula.");
                if (input.equalsIgnoreCase("exit")) return;
            }
        }

        int years;
        while (true) {
            System.out.print("Ani de experiență: ");
            String input = scanner.nextLine();
            try {
                years = Integer.parseInt(input);
                if (years < 0) throw new NumberFormatException();
                break;
            } catch (NumberFormatException e) {
                System.out.println("Ani invalidi. Introdu un număr valid sau tastează 'exit' pentru a anula.");
                if (input.equalsIgnoreCase("exit")) return;
            }
        }

        doctorService.updateDoctor(code, specialization, years);
        AuditService.getInstance().log("UPDATE_SPECIALIZATION: " + code);
        System.out.println("✅ Specializarea și experiența au fost actualizate.");
    }

    private void changeParafaCode() {
        System.out.print("Cod parafă vechi: ");
        String oldCode = scanner.nextLine();
        System.out.print("Cod parafă nou: ");
        String newCode = scanner.nextLine();

        doctorService.changeParafaCode(oldCode, newCode);
        AuditService.getInstance().log("CHANGE_PARAFACODE: " + oldCode + "->" + newCode);
        System.out.println("✅ Codul parafă a fost schimbat.");
    }

    private void deleteDoctor() {
        System.out.print("Cod parafă: ");
        String code = scanner.nextLine();
        System.out.print("Confirmă ștergerea (da/nu): ");
        String confirm = scanner.nextLine();
        if (confirm.equalsIgnoreCase("da")) {
            doctorService.deleteDoctor(code);
            AuditService.getInstance().log("DELETE_DOCTOR: " + code);
            System.out.println("✅ Doctor șters.");
        } else {
            System.out.println("❌ Ștergere anulată.");
        }
    }

    private void displayGroupedBySpecialization() {
        Map<Specialization, List<Doctor>> map = new HashMap<>();
        for (Doctor d : doctorService.getAllDoctors()) {
            map.computeIfAbsent(d.getSpecialization(), k -> new ArrayList<>()).add(d);
        }

        System.out.println("\n📚 Doctori grupați pe specializare:");
        for (var entry : map.entrySet()) {
            System.out.println("\n🔹 " + entry.getKey().name());
            for (Doctor d : entry.getValue()) {
                System.out.println(" - " + d.getFullName() + " (" + d.getParafaCode() + ")");
            }
        }

        AuditService.getInstance().log("GROUP_DOCTORS_BY_SPECIALIZATION");
    }

    private void displaySortedByExperience() {
        List<Doctor> sorted = new ArrayList<>(doctorService.getAllDoctors());
        sorted.sort(Comparator.comparingInt(Doctor::getYearsOfExperience).reversed());

        System.out.println("\n📈 Doctori ordonați după experiență:");
        for (Doctor d : sorted) {
            System.out.printf("- %s (%d ani)\n", d.getFullName(), d.getYearsOfExperience());
        }

        AuditService.getInstance().log("SORT_DOCTORS_BY_EXPERIENCE");
    }

    private void updatePersonalInfo() {
        System.out.print("Cod parafă: ");
        String code = scanner.nextLine();

        Optional<Doctor> doctorOpt = doctorService.getDoctorByParafaCode(code);
        if (doctorOpt.isEmpty()) {
            System.out.println("❌ Doctorul nu a fost găsit.");
            return;
        }

        Doctor doctor = doctorOpt.get();

        int opt;
        do {
            System.out.println("\n--- Ce doriți să actualizați? ---");
            System.out.println("1. Prenume");
            System.out.println("2. Nume");
            System.out.println("3. Email");
            System.out.println("4. Telefon");
            System.out.println("0. Înapoi");
            System.out.print("Opțiune: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            opt = scanner.nextInt();
            scanner.nextLine();

            switch (opt) {
                case 1 -> {
                    System.out.print("Prenume nou: ");
                    doctor.setFirstName(scanner.nextLine());
                }
                case 2 -> {
                    System.out.print("Nume nou: ");
                    doctor.setLastName(scanner.nextLine());
                }
                case 3 -> {
                    System.out.print("Email nou: ");
                    doctor.setEmail(scanner.nextLine());
                }
                case 4 -> {
                    System.out.print("Telefon nou: ");
                    doctor.setPhoneNumber(scanner.nextLine());
                }
                case 0 -> System.out.println("Revenire la meniul anterior.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }

            doctorService.updateDoctorPersonalInfo(doctor);
            AuditService.getInstance().log("UPDATE_DOCTOR_PERSONAL_INFO: " + code);
            System.out.println("✅ Informații actualizate.");

        } while (opt != 0);
    }
}
