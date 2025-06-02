package main.java.hospital.menu;

import main.java.hospital.model.Consultation;
import main.java.hospital.model.Diagnosis;
import main.java.hospital.model.Doctor;
import main.java.hospital.model.Patient;
import main.java.hospital.service.ConsultationService;
import main.java.hospital.service.DiagnosisService;
import main.java.hospital.service.DoctorService;
import main.java.hospital.service.PatientService;
import main.java.hospital.util.AuditService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ConsultationMenu {
    private final ConsultationService consultationService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DiagnosisService diagnosisService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsultationMenu(ConsultationService consultationService,
                            PatientService patientService,
                            DoctorService doctorService,
                            DiagnosisService diagnosisService) {
        this.consultationService = consultationService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.diagnosisService = diagnosisService;
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU CONSULTAȚII ===");
            System.out.println("1. Adaugă consultație");
            System.out.println("2. Afișează toate consultațiile");
            System.out.println("3. Caută consultații după CNP");
            System.out.println("4. Caută consultație după ID");
            System.out.println("5. Șterge consultație");
            System.out.println("6. Raport consultații per doctor");
            System.out.println("7. Actualizează o consultație");
            System.out.println("0. Înapoi");
            System.out.print("Opțiune: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> createConsultation();
                case 2 -> consultationService.displayAllConsultations();
                case 3 -> searchByCnp();
                case 4 -> searchById();
                case 5 -> deleteById();
                case 6 -> consultationService.reportConsultationsPerDoctor();
                case 7 -> updateConsultation();
                case 0 -> System.out.println("Revenire la meniul principal.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void createConsultation() {
        try {
            System.out.print("CNP pacient: ");
            String cnp = scanner.nextLine();
            Optional<Patient> optionalPatient = patientService.getPatientByCnp(cnp);
            if (optionalPatient.isEmpty()) {
                System.out.println("❌ Pacientul nu a fost găsit.");
                return;
            }
            Patient patient = optionalPatient.get();

            System.out.print("Cod parafă doctor: ");
            String code = scanner.nextLine();
            Optional<Doctor> optionalDoctor = doctorService.getDoctorByParafaCode(code);
            if (optionalDoctor.isEmpty()) {
                System.out.println("❌ Doctorul nu a fost găsit.");
                return;
            }
            System.out.print("Nume diagnostic: ");
            String name = scanner.nextLine();
            Optional<Diagnosis> optionalDiagnosis = diagnosisService.findDiagnosisByName(name);

            Diagnosis diagnosis;
            if (optionalDiagnosis.isEmpty()) {
                System.out.println("🔧 Diagnostic nou...");
                System.out.print("Descriere diagnostic: ");
                String desc = scanner.nextLine();
                System.out.print("Dată diagnostic (yyyy-MM-dd): ");
                LocalDate diagDate = LocalDate.parse(scanner.nextLine());
                int medRecId = patientService.getMedicalRecordForPatient(cnp).getId();

                Doctor doctor = optionalDoctor.get();
                diagnosis = new Diagnosis(name, desc, diagDate, doctor, medRecId);

            } else {
                diagnosis = optionalDiagnosis.get();
            }

            System.out.print("Dată consultație (yyyy-MM-dd): ");
            LocalDate date = LocalDate.parse(scanner.nextLine());

            System.out.print("Observații (opțional): ");
            String notes = scanner.nextLine();

            Doctor doctor = optionalDoctor.get();
            Consultation consultation = consultationService.createConsultation(patient, doctor, date, diagnosis, notes);
            System.out.println("✅ Consultație înregistrată cu ID: " + consultation.getId());
            AuditService.getInstance().log("CREATE_CONSULTATION: ID=" + consultation.getId());

        } catch (Exception e) {
            System.out.println("❌ Eroare: " + e.getMessage());
        }
    }

    private void searchByCnp() {
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();
        List<Consultation> list = consultationService.getConsultationsForPatient(cnp);
        if (list.isEmpty()) {
            System.out.println("❌ Nicio consultație găsită.");
        } else {
            list.forEach(System.out::println);
        }
    }

    private void searchById() {
        System.out.print("ID consultație: ");
        while (!scanner.hasNextInt()) {
            System.out.print("⚠️ Introdu un ID valid: ");
            scanner.next();
        }
        int id = scanner.nextInt();
        scanner.nextLine();

        Optional<Consultation> optional = consultationService.getConsultationById(id);
        if (optional.isPresent()) {
            System.out.println(optional.get());
        } else {
            System.out.println("❌ Consultația nu a fost găsită.");
        }
    }

    private void deleteById() {
        System.out.print("ID consultație: ");
        while (!scanner.hasNextInt()) {
            System.out.print("⚠️ Introdu un ID valid: ");
            scanner.next();
        }
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Confirmi ștergerea? (da/nu): ");
        String confirm = scanner.nextLine();
        if (!confirm.equalsIgnoreCase("da")) return;

        boolean result = consultationService.deleteConsultationById(id);
        if (result) {
            System.out.println("🗑️ Consultația a fost ștearsă.");
            AuditService.getInstance().log("DELETE_CONSULTATION: ID=" + id);
        } else {
            System.out.println("❌ Consultația nu a fost găsită.");
        }
    }

    private void updateConsultation() {
        System.out.print("ID consultație de actualizat: ");
        while (!scanner.hasNextInt()) {
            System.out.print("⚠️ Introdu un ID valid: ");
            scanner.next();
        }
        int id = scanner.nextInt();
        scanner.nextLine();

        Optional<Consultation> optional = consultationService.getConsultationById(id);
        if (optional.isEmpty()) {
            System.out.println("❌ Consultația nu a fost găsită.");
            return;
        }

        Consultation consultation = optional.get();

        int opt;
        do {
            System.out.println("\n--- Ce dorești să actualizezi? ---");
            System.out.println("1. Doctor");
            System.out.println("2. Diagnostic");
            System.out.println("3. Dată consultație");
            System.out.println("4. Observații");
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
                    System.out.print("Cod parafă nou: ");
                    String newCode = scanner.nextLine();
                    Optional<Doctor> newDoctor = doctorService.getDoctorByParafaCode(newCode);
                    if (newDoctor.isPresent()) {
                        consultation.setDoctor(newDoctor.get());
                        System.out.println("✅ Doctor actualizat.");
                    } else {
                        System.out.println("❌ Doctorul nu a fost găsit.");
                    }
                }
                case 2 -> {
                    System.out.print("Nume diagnostic: ");
                    String diagName = scanner.nextLine();
                    Optional<Diagnosis> optionalDiagnosis = diagnosisService.findDiagnosisByName(diagName);
                    if (optionalDiagnosis.isPresent()) {
                        System.out.print("Nume diagnostic nou: ");
                        String diag = scanner.nextLine();
                        diagnosisService.updateDiagnosisNameById(optionalDiagnosis.get().getId(), diag);
                        System.out.println("✅ Diagnostic actualizat.");
                    } else {
                        System.out.println("❌ Diagnostic inexistent");
                    }
                }
                case 3 -> {
                    System.out.print("Noua dată (yyyy-MM-dd): ");
                    try {
                        LocalDate newDate = LocalDate.parse(scanner.nextLine());
                        consultation.setDate(newDate);
                        System.out.println("✅ Dată actualizată.");
                    } catch (Exception e) {
                        System.out.println("❌ Format dată invalid.");
                    }
                }
                case 4 -> {
                    System.out.print("Noile observații: ");
                    consultation.setNotes(scanner.nextLine());
                    System.out.println("✅ Observații actualizate.");
                }
                case 0 -> System.out.println("Înapoi la meniul anterior.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (opt != 0);

        consultationService.saveConsultation(consultation); // trebuie implementat
        AuditService.getInstance().log("UPDATE_CONSULTATION: ID=" + consultation.getId());
        System.out.println("✅ Consultația a fost actualizată.");
    }
}
