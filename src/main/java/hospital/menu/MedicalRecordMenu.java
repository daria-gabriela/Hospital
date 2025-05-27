package main.java.hospital.menu;

import main.java.hospital.model.*;
import main.java.hospital.service.*;
import main.java.hospital.util.AuditService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MedicalRecordMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final PatientService patientService = new PatientService();
    private final DiagnosisService diagnosisService = new DiagnosisService();
    private final DoctorService doctorService = new DoctorService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final MedicalRecordService medicalRecordService = new MedicalRecordService();

    public void showMenu() {
        System.out.print("🔎 Introduceți CNP-ul pacientului: ");
        String cnp = scanner.nextLine();

        Optional<Patient> optional = patientService.getPatientByCnp(cnp);
        if (optional.isEmpty()) {
            System.out.println("❌ Pacientul nu a fost găsit.");
            return;
        }

        Patient patient = optional.get();
        MedicalRecord record = patient.getMedicalRecord();

        // 🔄 Încarcă doctori și diagnostice actualizate
        diagnosisService.loadFromDatabase(doctorService.getAllDoctors());

        if (record != null) {
            List<Diagnosis> diagnoses = diagnosisService.getDiagnosesByMedicalRecordId(record.getId());
            record.getDiagnoses().clear();
            record.getDiagnoses().addAll(diagnoses);
        }

        int option;
        do {
            record = patient.getMedicalRecord(); // actualizare în timp real

            System.out.println("\n=== MENIU FIȘĂ MEDICALĂ — " + patient.getFullName() + " ===");
            if (record == null) {
                System.out.println("⚠️ Nu există fișă medicală asociată.");
            } else {
                System.out.println("📄 Fișă ID: " + record.getId() + " | Creată la: " + record.getCreationDate());
            }

            System.out.println("1. Afișează fișa medicală și diagnosticele");
            System.out.println("2. Modifică data fișei medicale");
                ///3. Șterge complet fișa medicală
            System.out.println("3. Meniu Diagnostic");
                ///5. Creează fișa medicală
            System.out.println("0. Înapoi");
            System.out.print("Opțiune: ");
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> showMedicalRecord(record, cnp);
                case 2 -> updateRecordDate(record);
                //case 3 -> deleteMedicalRecord(record, patient, cnp);
                case 3 -> openDiagnosisMenu(record, patient);
               /// case 5 -> createMedicalRecord(patient, cnp);
                case 0 -> System.out.println("↩️ Revenire la meniul anterior.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }

        } while (option != 0);
    }

    private void showMedicalRecord(MedicalRecord record, String cnp) {
        if (record != null) {
            System.out.println(record);
            System.out.println("📋 Diagnostice asociate:");
            record.getDiagnoses().forEach(System.out::println);
            AuditService.getInstance().log("VIEW_MEDICAL_RECORD: " + cnp);
        } else {
            System.out.println("❌ Fișa medicală nu există.");
        }
    }

    private void updateRecordDate(MedicalRecord record) {
        if (record == null) {
            System.out.println("❌ Nu există fișă de modificat.");
            return;
        }

        System.out.print("📅 Noua dată (YYYY-MM-DD): ");
        String input = scanner.nextLine();
        try {
            LocalDate newDate = LocalDate.parse(input);
            boolean success = medicalRecordService.updateMedicalRecordDate(record.getId(), newDate);
            if (success) {
                record.setCreationDate(newDate);
                System.out.println("✅ Dată actualizată cu succes.");
            } else {
                System.out.println("❌ Actualizarea a eșuat.");
            }
        } catch (Exception e) {
            System.out.println("❌ Format de dată invalid.");
        }
    }

    private void deleteMedicalRecord(MedicalRecord record, Patient patient, String cnp) {
        if (record == null) {
            System.out.println("❌ Nu există fișă de șters.");
            return;
        }

        System.out.print("❗ Confirmați ștergerea? (da/nu): ");
        String confirm = scanner.nextLine();
        if (!confirm.equalsIgnoreCase("da")) return;

        boolean success = medicalRecordService.deleteMedicalRecord(record.getId());
        if (success) {
            patient.setMedicalRecord(null);
            System.out.println("✅ Fișa medicală a fost ștearsă.");
            AuditService.getInstance().log("DELETE_MEDICAL_RECORD: " + cnp);
        } else {
            System.out.println("❌ Eroare la ștergerea fișei.");
        }
    }

    private void openDiagnosisMenu(MedicalRecord record, Patient patient) {
        if (record == null) {
            System.out.println("❌ Creează mai întâi o fișă medicală.");
            return;
        }

        List<Diagnosis> updatedDiagnoses = diagnosisService.getDiagnosesByMedicalRecordId(record.getId());
        record.getDiagnoses().clear();
        record.getDiagnoses().addAll(updatedDiagnoses);

        DiagnosisMenu diagnosisMenu = new DiagnosisMenu(patient, diagnosisService, doctorService, prescriptionService);
        diagnosisMenu.showMenu();
    }

    private void createMedicalRecord(Patient patient, String cnp) {
        if (patient.getMedicalRecord() != null) {
            System.out.println("❌ Fișa există deja.");
            return;
        }

        MedicalRecord newRecord = medicalRecordService.addMedicalRecord(LocalDate.now());
        if (newRecord != null) {
            patient.setMedicalRecord(newRecord);
            System.out.println("✅ Fișa medicală a fost creată cu ID: " + newRecord.getId());
            AuditService.getInstance().log("CREATE_MEDICAL_RECORD: " + cnp);
        } else {
            System.out.println("❌ Eroare la creare.");
        }
    }
}
