package main.java.hospital.menu;

import main.java.hospital.model.BloodGroup;
import main.java.hospital.model.RhType;
import main.java.hospital.service.InvoiceService;
import main.java.hospital.service.MedicalRecordService;
import main.java.hospital.service.PatientService;
import main.java.hospital.util.AuditService;

import java.util.Scanner;
import java.util.regex.Pattern;

public class PatientMenu {
    private final MedicalRecordMenu medicalRecordMenu = new MedicalRecordMenu();
    private final Scanner scanner = new Scanner(System.in);
    private final PatientService patientService;
    private final MedicalRecordService medicalRecordService;
    private final InvoiceService invoiceService;

    public PatientMenu(PatientService patientService, MedicalRecordService medicalRecordService, InvoiceService invoiceService) {
        this.patientService = patientService;
        this.medicalRecordService = medicalRecordService;
        this.invoiceService = invoiceService;
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU PACIENȚI ===");
            System.out.println("1. Adaugă pacient");
            System.out.println("2. Afișează toți pacienții");
            System.out.println("3. Afișează un pacient");
            System.out.println("4. Afișează în funcție de categoria de vârstă");
            System.out.println("5. Actualizează pacient");
            System.out.println("6. Șterge pacient");
            System.out.println("7. Vezi fișa medicală a pacientului");
            System.out.println("8. Afișează facturile unui pacient");
            System.out.println("9. Total de plată al unui pacient");
            System.out.println("10. Afișează pacienții după grupa sanguină");
            System.out.println("11. Afișează pacienții după RH");
            System.out.println("0. Înapoi");
            System.out.print("Opțiune: ");
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> addPatient();
                case 2 -> patientService.displayAllPatients();
                case 3 -> patientService.displayPatientByCnp();
                case 4 -> patientService.displayPatientsByAgeCategory();
                case 5 -> updatePatient();
                case 6 -> deletePatient();
                case 7 -> medicalRecordMenu.showMenu();
                case 8 -> viewInvoices();
                case 9 -> viewUnpaidAmount();
                case 10 -> displayPatientsByBloodGroup();
                case 11 -> displayPatientsByRhType();
                case 0 -> System.out.println("Revenire la meniul principal.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void addPatient() {
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
                    if (!email.matches("^.+@.+\\..+$")) {
                        System.out.println("Email invalid. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        if (email.equalsIgnoreCase("exit")) return;
                    } else break;
                } while (true);

                String phone;
                do {
                    System.out.print("Telefon: ");
                    phone = scanner.nextLine();
                    if (!Pattern.matches("^[0-9+]{10,15}$", phone)) {
                        System.out.println("Număr de telefon invalid. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        if (phone.equalsIgnoreCase("exit")) return;
                    } else break;
                } while (true);

                String cnp;
                do {
                    System.out.print("CNP: ");
                    cnp = scanner.nextLine();
                    if (!cnp.matches("\\d{13}")) {
                        System.out.println("CNP invalid. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        if (cnp.equalsIgnoreCase("exit")) return;
                    } else break;
                } while (true);

                System.out.print("Adresă: ");
                String address = scanner.nextLine();

                BloodGroup bg;
                while (true) {
                    try {
                        System.out.print("Grupa sanguină (A, B, AB, O): ");
                        bg = BloodGroup.valueOf(scanner.nextLine().toUpperCase());
                        break;
                    } catch (Exception e) {
                        System.out.println("Grupă sanguină invalidă. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        String response = scanner.nextLine();
                        if (response.equalsIgnoreCase("exit")) return;
                    }
                }

                RhType rh;
                while (true) {
                    try {
                        System.out.print("RH (+ / - sau POSITIVE / NEGATIVE): ");
                        rh = RhType.fromString(scanner.nextLine());
                        break;
                    } catch (Exception e) {
                        System.out.println("RH invalid. Încearcă din nou sau tastează 'exit' pentru a anula.");
                        String response = scanner.nextLine();
                        if (response.equalsIgnoreCase("exit")) return;
                    }
                }

                patientService.addPatient(firstName, lastName, email, phone, cnp, address, bg, rh);
                System.out.println("✅ Pacient adăugat cu succes!");
                AuditService.getInstance().log("CREATE_PATIENT: " + cnp);
                break;

            } catch (IllegalArgumentException e) {
                System.out.println("❌ Date invalide: " + e.getMessage());
                System.out.println("🔁 Reîncearcă introducerea tuturor datelor...\n");
            } catch (Exception e) {
                System.out.println("❌ Eroare critică: " + e.getMessage());
                break;
            }
        }
    }

    private void displayPatientsByBloodGroup() {
        System.out.print("Introduceți grupa sanguină (A, B, AB, O): ");
        String input = scanner.nextLine().toUpperCase();
        try {
            BloodGroup group = BloodGroup.valueOf(input);
            System.out.println("📋 Pacienți cu grupa " + group + ":");
            patientService.getAllPatients().stream()
                    .filter(p -> group.equals(p.getBloodGroup()))
                    .forEach(System.out::println);

            AuditService.getInstance().log("FILTER_PATIENTS_BY_BLOOD_GROUP: " + group);
        } catch (IllegalArgumentException e) {
            System.out.println("⚠️ Grupă sanguină invalidă.");
        }
    }

    private void displayPatientsByRhType() {
        System.out.print("Introduceți RH (+ / - sau POSITIVE / NEGATIVE): ");
        String input = scanner.nextLine();
        try {
            RhType rh = RhType.fromString(input);
            System.out.println("📋 Pacienți cu RH " + rh + ":");
            patientService.getAllPatients().stream()
                    .filter(p -> rh.equals(p.getRhType()))
                    .forEach(System.out::println);

            AuditService.getInstance().log("FILTER_PATIENTS_BY_RH: " + rh);
        } catch (IllegalArgumentException e) {
            System.out.println("⚠️ Tip RH invalid.");
        }
    }

    private void updatePatient() {
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();

        int opt;
        do {
            System.out.println("\n--- Ce doriți să actualizați? ---");
            System.out.println("1. Adresă");
            System.out.println("2. Grupa sanguină");
            System.out.println("3. RH");
            System.out.println("4. Telefon");
            System.out.println("5. Email");
            System.out.println("6. Nume");
            System.out.println("7. Prenume");
            System.out.println("0. Înapoi");
            System.out.print("Opțiune: ");
            opt = scanner.nextInt();
            scanner.nextLine();

            switch (opt) {
                case 1 -> {
                    System.out.print("Noua adresă: ");
                    String address = scanner.nextLine();
                    patientService.updatePatientAddress(cnp, address);
                }
                case 2 -> {
                    System.out.print("Grupa sanguină (A, B, AB, O): ");
                    BloodGroup bg = BloodGroup.valueOf(scanner.nextLine().toUpperCase());
                    patientService.updatePatientBloodGroup(cnp, bg);
                }
                case 3 -> {
                    System.out.print("RH (+ / - sau POSITIVE / NEGATIVE): ");
                    RhType rh = RhType.fromString(scanner.nextLine());
                    patientService.updatePatientRhType(cnp, rh);
                }
                case 4 -> {
                    System.out.print("Număr telefon: ");
                    String phone = scanner.nextLine();
                    patientService.updatePhoneNumber(cnp, phone);
                }
                case 5 -> {
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    patientService.updatePatientEmail(cnp, email);
                }
                case 6 -> {
                    System.out.print("Nume nou: ");
                    String lastName = scanner.nextLine();
                    patientService.updatePatientName(cnp, lastName);
                }
                case 7 -> {
                    System.out.print("Prenume nou: ");
                    String firstName = scanner.nextLine();
                    patientService.updatePatientFirstName(cnp, firstName);
                }
                case 0 -> System.out.println("Revenire la meniul anterior.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }

        } while (opt != 0);
    }

    private void deletePatient() {
        System.out.print("CNP pacient de șters: ");
        String cnp = scanner.nextLine();
        if (patientService.deletePatient(cnp)) {
            System.out.println("✅ Pacient șters cu succes.");
        } else {
            System.out.println("❌ Pacientul nu a fost găsit sau nu a putut fi șters.");
        }
    }

    private void viewMedicalRecord() {
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();
        System.out.println(patientService.getMedicalRecordForPatient(cnp));
        AuditService.getInstance().log("READ_MEDICAL_RECORD: " + cnp);
    }

    private void viewInvoices() {
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();
        patientService.displayInvoicesForPatient(cnp);
        AuditService.getInstance().log("READ_INVOICES: " + cnp);
    }

    private void viewUnpaidAmount() {
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();
        double amount = patientService.getTotalUnpaidAmountForPatient(cnp);
        System.out.println("💰 Total de plată: " + amount + " lei");
        AuditService.getInstance().log("CALCULATE_TOTAL_UNPAID: " + cnp);
    }
}