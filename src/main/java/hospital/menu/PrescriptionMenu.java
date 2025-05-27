package main.java.hospital.menu;

import main.java.hospital.model.*;
import main.java.hospital.service.PrescriptionService;
import main.java.hospital.util.AuditService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class PrescriptionMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final Patient currentPatient;
    private final PrescriptionService prescriptionService;

    public PrescriptionMenu(PrescriptionService prescriptionService, Patient currentPatient) {
        this.prescriptionService = prescriptionService;
        this.currentPatient = currentPatient;
    }

    public void showMenu() {
        prescriptionService.reloadPrescriptions();
        int option;
        do {
            System.out.println("\n=== MENIU REȚETE [" + currentPatient.getFullName() + "] ===");
            System.out.println("1. Afișează toate rețetele");
            System.out.println("2. Caută după medicament");
            System.out.println("3. Șterge rețetă după ID");
            System.out.println("4. Actualizează dozaj");
            System.out.println("5. Actualizează orice atribut");
            System.out.println("6. Rețete active");
            System.out.println("7. Rețete de reînnoit azi");
            System.out.println("8. Adaugă rețetă legată de diagnostic");
            System.out.println("0. Înapoi");
            System.out.print("Alege opțiune: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> prescriptionService.printAllPrescriptions();
                case 2 -> searchByMedication();
                case 3 -> deleteById();
                case 4 -> updateDosage();
                case 5 -> updateAttribute();
                case 6 -> showActive();
                case 7 -> showToRenewToday();
                case 8 -> addPrescriptionToDiagnosis();
                case 0 -> System.out.println("↩️ Revenire la meniul anterior.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void searchByMedication() {
        System.out.print("🔍 Nume medicament: ");
        String med = scanner.nextLine();
        prescriptionService.searchByMedication(med);
        AuditService.getInstance().log("SEARCH_PRESCRIPTION_BY_MED: " + med);
    }

    private void deleteById() {
        System.out.print("ID rețetă: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        boolean deleted = prescriptionService.deletePrescription(id);
        if (deleted) {
            System.out.println("🗑️ Rețetă ștearsă.");
            AuditService.getInstance().log("DELETE_PRESCRIPTION: ID=" + id);
        } else {
            System.out.println("❌ Rețetă inexistentă.");
        }
    }

    private void updateDosage() {
        System.out.print("ID rețetă: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Noul dozaj: ");
        String newDosage = scanner.nextLine();
        boolean updated = prescriptionService.updateDosage(id, newDosage);
        if (updated) {
            System.out.println("✅ Dozaj actualizat.");
            AuditService.getInstance().log("UPDATE_DOSAGE: ID=" + id);
        } else {
            System.out.println("❌ Rețeta nu a fost găsită.");
        }
    }

    private void updateAttribute() {
        System.out.print("ID rețetă: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        Optional<Prescription> opt = prescriptionService.findById(id);
        if (opt.isEmpty()) {
            System.out.println("❌ Rețeta nu a fost găsită.");
            return;
        }

        Prescription p = opt.get();
        System.out.println("""
            Ce dorești să modifici?
            1. Medicament
            2. Dozaj
            3. Data eliberare
            4. Început tratament
            5. Sfârșit tratament
            6. Reînnoire automată
            7. Data reînnoirii
            """);
        System.out.print("Alegere: ");
        int attr = scanner.nextInt();
        scanner.nextLine();

        try {
            switch (attr) {
                case 1 -> {
                    System.out.print("🔄 Medicament nou: ");
                    p.setMedication(scanner.nextLine());
                }
                case 2 -> {
                    System.out.print("🔄 Dozaj nou: ");
                    p.setDosage(scanner.nextLine());
                }
                case 3 -> {
                    System.out.print("📅 Data eliberare (YYYY-MM-DD): ");
                    p.setDateIssued(LocalDate.parse(scanner.nextLine()));
                }
                case 4 -> {
                    System.out.print("📅 Început tratament: ");
                    p.setStartDate(LocalDate.parse(scanner.nextLine()));
                }
                case 5 -> {
                    System.out.print("📅 Sfârșit tratament: ");
                    p.setEndDate(LocalDate.parse(scanner.nextLine()));
                }
                case 6 -> {
                    System.out.print("♻️ Reînnoire automată (true/false): ");
                    p.setAutoRenew(Boolean.parseBoolean(scanner.nextLine()));
                }
                case 7 -> {
                    System.out.print("📅 Data reînnoire: ");
                    p.setRenewDate(LocalDate.parse(scanner.nextLine()));
                }
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
            System.out.println("✅ Atribut actualizat.");
            AuditService.getInstance().log("UPDATE_PRESCRIPTION_ATTR: ID=" + id);
        } catch (Exception e) {
            System.out.println("❌ Eroare: " + e.getMessage());
        }
    }

    private void showActive() {
        List<Prescription> active = prescriptionService.getActivePrescriptions();
        if (active.isEmpty()) {
            System.out.println("📭 Nicio rețetă activă.");
        } else {
            active.forEach(System.out::println);
        }
    }

    private void showToRenewToday() {
        LocalDate today = LocalDate.now();
        List<Prescription> toRenew = prescriptionService.getPrescriptionsToRenew(today);
        if (toRenew.isEmpty()) {
            System.out.println("📭 Nicio rețetă de reînnoit azi.");
        } else {
            toRenew.forEach(System.out::println);
        }
    }

    private void addPrescriptionToDiagnosis() {
        System.out.print("🔎 Nume diagnostic: ");
        String diagName = scanner.nextLine();

        Optional<Diagnosis> optional = currentPatient.getMedicalRecord().getDiagnoses().stream()
                .filter(d -> d.getName().equalsIgnoreCase(diagName))
                .findFirst();

        if (optional.isEmpty()) {
            System.out.println("❌ Diagnosticul nu a fost găsit în fișa pacientului.");
            return;
        }

        Diagnosis diagnosis = optional.get();

        try {
            Prescription p = createPrescriptionFromInput();
            p.setDiagnosisId(diagnosis.getId());

            diagnosis.addPrescription(p);
            prescriptionService.addPrescription(p);
            System.out.println("✅ Rețetă adăugată pentru diagnosticul: " + diagnosis.getName());
            AuditService.getInstance().log("ADD_PRESCRIPTION_TO_DIAGNOSIS: " + diagnosis.getName());
        } catch (SQLException e) {
            System.err.println("❌ Eroare la salvare: " + e.getMessage());
        }
    }

    private Prescription createPrescriptionFromInput() {
        System.out.print("Medicament: ");
        String medication = scanner.nextLine();

        System.out.print("Dozaj: ");
        String dosage = scanner.nextLine();

        System.out.print("Dată eliberare (YYYY-MM-DD): ");
        LocalDate dateIssued = LocalDate.parse(scanner.nextLine());

        System.out.print("Început tratament (YYYY-MM-DD): ");
        LocalDate start = LocalDate.parse(scanner.nextLine());

        System.out.print("Sfârșit tratament (YYYY-MM-DD): ");
        LocalDate end = LocalDate.parse(scanner.nextLine());

        System.out.print("Reînnoire automată (true/false): ");
        boolean autoRenew = Boolean.parseBoolean(scanner.nextLine());

        LocalDate renewDate = null;
        if (autoRenew) {
            System.out.print("Dată reînnoire (YYYY-MM-DD): ");
            renewDate = LocalDate.parse(scanner.nextLine());
        }

        return new Prescription(medication, dosage, dateIssued, start, end, autoRenew, renewDate);
    }
}
