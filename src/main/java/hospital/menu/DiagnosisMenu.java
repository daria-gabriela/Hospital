package main.java.hospital.menu;

import main.java.hospital.model.*;
import main.java.hospital.service.DiagnosisService;
import main.java.hospital.service.DoctorService;
import main.java.hospital.service.PrescriptionService;
import main.java.hospital.util.AuditService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class DiagnosisMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final Patient currentPatient;
    private final DiagnosisService diagnosisService;
    private final DoctorService doctorService;
    private final PrescriptionService prescriptionService;
    private final PrescriptionMenu prescriptionMenu;

    public DiagnosisMenu(Patient currentPatient, DiagnosisService diagnosisService, DoctorService doctorService, PrescriptionService prescriptionService) {
        this.currentPatient = currentPatient;
        this.diagnosisService = diagnosisService;
        this.doctorService = doctorService;
        this.prescriptionService = prescriptionService;
        this.prescriptionMenu = new PrescriptionMenu(prescriptionService, currentPatient);
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU DIAGNOSTICE ===");
            System.out.println("1. Adaugă diagnostic");
            System.out.println("2. Afișează toate diagnosticele");
            System.out.println("3. Șterge diagnostic");
            System.out.println("4. Afișează prescripții pentru diagnostic");
            System.out.println("5. Actualizează descriere/dată");
            System.out.println("6. Actualizează doctor asociat");
            System.out.println("7. Actualizează numele diagnostic");
            System.out.println("8. Adaugă prescripție la diagnostic");
            System.out.println("9. Meniu complet prescripții");
            System.out.println("0. Înapoi");
            System.out.print("Opțiune: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> addDiagnosis();
                case 2 -> showAllDiagnoses();
                case 3 -> deleteDiagnosis();
                case 4 -> showPrescriptionsForDiagnosis();
                case 5 -> updateDiagnosis();
                case 6 -> updateDoctorForDiagnosis();
                case 7 -> updateDiagnosisName();
                case 8 -> {
                    try {
                        addPrescriptionToDiagnosis();
                    } catch (SQLException e) {
                        System.out.println("❌ Eroare la adăugarea prescripției: " + e.getMessage());
                    }
                }
                case 9 -> prescriptionMenu.showMenu();
                case 0 -> System.out.println("Revenire...");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void showAllDiagnoses() {
        MedicalRecord record = currentPatient.getMedicalRecord();
        if (record == null) {
            System.out.println("❌ Nu există fișă medicală.");
            return;
        }

        List<Diagnosis> diagList = diagnosisService.getDiagnosesByMedicalRecordId(record.getId());
        record.getDiagnoses().clear();
        record.getDiagnoses().addAll(diagList);

        if (diagList.isEmpty()) {
            System.out.println("📭 Niciun diagnostic pentru acest pacient.");
        } else {
            diagList.forEach(System.out::println);
        }

        AuditService.getInstance().log("DISPLAY_DIAGNOSES: " + currentPatient.getCnp());
    }

    private void addDiagnosis() {
        System.out.print("Nume diagnostic: ");
        String name = scanner.nextLine();
        System.out.print("Descriere: ");
        String desc = scanner.nextLine();
        System.out.print("Dată diagnostic (AAAA-LL-ZZ): ");
        LocalDate date = LocalDate.parse(scanner.nextLine());

        Doctor doctor = chooseOrCreateDoctor();
        int medicalRecordId = currentPatient.getMedicalRecord().getId();

        Diagnosis d = diagnosisService.addDiagnosis(name, desc, date, doctor, medicalRecordId);
        currentPatient.getMedicalRecord().addDiagnosis(d);
        System.out.println("✅ Diagnostic adăugat.");
        AuditService.getInstance().log("CREATE_DIAGNOSIS: " + name);
    }

    private void deleteDiagnosis() {
        System.out.print("Nume diagnostic: ");
        String name = scanner.nextLine();

        Diagnosis d = findDiagnosisByNameLocal(name);
        if (d != null) {
            System.out.print("Confirmi ștergerea? (da/nu): ");
            String confirm = scanner.nextLine();
            if (!confirm.equalsIgnoreCase("da")) return;

            currentPatient.getMedicalRecord().getDiagnoses().remove(d);
            diagnosisService.deleteDiagnosis(name);
            System.out.println("✅ Diagnostic șters din fișă și din DB.");
            AuditService.getInstance().log("DELETE_DIAGNOSIS: " + name);
        } else {
            System.out.println("❌ Diagnosticul nu a fost găsit.");
        }
    }

    private void showPrescriptionsForDiagnosis() {
        System.out.print("Nume diagnostic: ");
        String name = scanner.nextLine();
        diagnosisService.displayPrescriptionsForDiagnosis(name);
    }

    private void updateDiagnosis() {
        System.out.print("Numele diagnostic de actualizat: ");
        String name = scanner.nextLine();
        Diagnosis d = findDiagnosisByNameLocal(name);

        if (d == null) {
            System.out.println("❌ Diagnosticul nu a fost găsit.");
            return;
        }

        System.out.print("Noua descriere (ENTER pentru neschimbat): ");
        String desc = scanner.nextLine();
        if (!desc.isBlank()) d.setDescription(desc);

        System.out.print("Noua dată (AAAA-LL-ZZ) sau ENTER: ");
        String dateStr = scanner.nextLine();
        if (!dateStr.isBlank()) {
            try {
                d.setDate(LocalDate.parse(dateStr));
            } catch (DateTimeParseException e) {
                System.out.println("⚠️ Dată invalidă.");
            }
        }

        boolean updated = diagnosisService.updateDiagnosisInDatabase(name, d.getDescription(), d.getDate());
        System.out.println(updated ? "✅ Actualizat în DB." : "❌ Eroare DB.");
        AuditService.getInstance().log("UPDATE_DIAGNOSIS_DATE_DESC: " + name);
    }

    private void updateDiagnosisName() {
        System.out.print("Numele actual al diagnosticului: ");
        String currentName = scanner.nextLine();
        Diagnosis d = findDiagnosisByNameLocal(currentName);

        if (d == null) {
            System.out.println("❌ Diagnosticul nu a fost găsit.");
            return;
        }

        System.out.print("Noul nume: ");
        String newName = scanner.nextLine();
        d.setName(newName);

        boolean updated = diagnosisService.updateDiagnosisNameById(d.getId(), newName);
        System.out.println(updated ? "✅ Numele actualizat." : "❌ Eroare DB.");
        AuditService.getInstance().log("UPDATE_DIAGNOSIS_NAME: " + currentName);
    }

    private void updateDoctorForDiagnosis() {
        System.out.print("Nume diagnostic: ");
        String name = scanner.nextLine();
        Diagnosis d = findDiagnosisByNameLocal(name);

        if (d == null) {
            System.out.println("❌ Diagnosticul nu a fost găsit.");
            return;
        }

        Doctor doctor = chooseOrCreateDoctor();
        d.setDoctor(doctor);

        boolean updated = diagnosisService.updateDiagnosisDoctor(name, doctor);
        System.out.println(updated ? "✅ Doctor actualizat." : "❌ Eroare DB.");
        AuditService.getInstance().log("UPDATE_DIAGNOSIS_DOCTOR: " + name);
    }

    private void addPrescriptionToDiagnosis() throws SQLException {
        System.out.print("Nume diagnostic: ");
        String name = scanner.nextLine();
        Diagnosis diagnosis = findDiagnosisByNameLocal(name);

        if (diagnosis == null) {
            System.out.println("❌ Diagnosticul nu a fost găsit.");
            return;
        }

        System.out.print("Medicament: ");
        String medication = scanner.nextLine();
        System.out.print("Dozaj: ");
        String dosage = scanner.nextLine();
        System.out.print("Data emiterii (AAAA-LL-ZZ): ");
        LocalDate issueDate = LocalDate.parse(scanner.nextLine());
        System.out.print("Început tratament (AAAA-LL-ZZ): ");
        LocalDate startDate = LocalDate.parse(scanner.nextLine());
        System.out.print("Sfârșit tratament (AAAA-LL-ZZ): ");
        LocalDate endDate = LocalDate.parse(scanner.nextLine());
        System.out.print("Reînnoire automată (true/false): ");
        boolean autoRenew = Boolean.parseBoolean(scanner.nextLine());

        LocalDate renewDate = null;
        if (autoRenew) {
            System.out.print("Data reînnoirii (AAAA-LL-ZZ): ");
            renewDate = LocalDate.parse(scanner.nextLine());
        }

        Prescription p = new Prescription(medication, dosage, issueDate, startDate, endDate, autoRenew, renewDate);
        boolean success = diagnosisService.addPrescriptionToDiagnosis(diagnosis, p, prescriptionService);
        if (success) {
            System.out.println("✅ Rețetă adăugată.");
            AuditService.getInstance().log("CREATE_PRESCRIPTION: " + medication);
        } else {
            System.out.println("❌ Eroare la adăugarea rețetei.");
        }

        prescriptionService.addPrescription(p);
        System.out.println("✅ Rețetă adăugată.");
        AuditService.getInstance().log("CREATE_PRESCRIPTION: " + medication);
    }

    private Diagnosis findDiagnosisByNameLocal(String name) {
        return currentPatient.getMedicalRecord()
                .getDiagnoses()
                .stream()
                .filter(d -> d.getName().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    private Doctor chooseOrCreateDoctor() {
        Doctor doctor = null;
        List<Doctor> allDoctors = doctorService.getAllDoctors();
        if (!allDoctors.isEmpty()) {
            System.out.println("\n📋 Doctori existenți:");
            for (int i = 0; i < allDoctors.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, allDoctors.get(i).getFullName());
            }
            System.out.print("Vrei să alegi un doctor existent sau să adaugi unul nou? (E/N): ");
            String choice = scanner.nextLine();

            if (choice.equalsIgnoreCase("E")) {
                System.out.print("Introdu numărul doctorului: ");
                int index = scanner.nextInt();
                scanner.nextLine();
                if (index >= 1 && index <= allDoctors.size()) {
                    doctor = allDoctors.get(index - 1);
                } else {
                    System.out.println("⚠️ Index invalid. Se va crea un doctor nou.");
                }
            }
        }

        if (doctor == null) {
            System.out.print("Prenume: ");
            String firstName = scanner.nextLine();
            System.out.print("Nume: ");
            String lastName = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Telefon: ");
            String phone = scanner.nextLine();

            System.out.println("Specializări disponibile:");
            for (Specialization spec : Specialization.values()) {
                System.out.println("- " + spec.name());
            }
            System.out.print("Specializare: ");
            Specialization specialization = Specialization.valueOf(scanner.nextLine().toUpperCase());

            System.out.print("Ani experiență: ");
            int experience = Integer.parseInt(scanner.nextLine());

            System.out.print("Cod parafă: ");
            String parafa = scanner.nextLine();

            doctor = doctorService.addDoctor(firstName, lastName, email, phone, specialization, experience, parafa);
        }

        return doctor;
    }
}