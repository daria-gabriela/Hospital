package main.java.hospital.menu;

import main.java.hospital.service.*;

import java.util.Scanner;

public class MainMenu {

    private static MainMenu instance;

    private final Scanner scanner = new Scanner(System.in);

    // Servicii
    private RoomService roomService;
    private MedicalDepartmentService departmentService;
    private PatientService patientService;
    private DoctorService doctorService;
    private DiagnosisService diagnosisService;
    private PrescriptionService prescriptionService;
    private MedicalRecordService medicalRecordService;
    private ConsultationService consultationService;
    private MedicalAppointmentService appointmentService;
    private NurseService nurseService;
    private InvoiceService invoiceService;

    // Meniuri
    private PatientMenu patientMenu;
    private DoctorMenu doctorMenu;
    private ConsultationMenu consultationMenu;
    private MedicalAppointmentMenu appointmentMenu;
    private MedicalDepartmentMenu departmentMenu;
    private InvoiceMenu invoiceMenu;
    private NurseMenu nurseMenu;

    private MainMenu() {
            // Inițializare servicii
        this.departmentService = MedicalDepartmentService.getInstance();
        this.roomService = new RoomService();

        // Pas 1: încarcă întâi DEPARTAMENTELE
        this.departmentService.loadDepartmentsOnly();

        // Pas 2: conectare servicii
        this.departmentService.setRoomService(this.roomService);
        this.roomService.setDepartmentService(this.departmentService);

        // Pas 3: încarcă camerele DUPĂ ce departamentele sunt gata
        this.roomService.loadRoomsFromDB();

        this.departmentService.initialize();

        // ✅ Inițializăm celelalte servicii
        this.patientService = new PatientService();
        this.doctorService = new DoctorService();
        this.diagnosisService = new DiagnosisService();
        this.prescriptionService = new PrescriptionService();
        this.medicalRecordService = new MedicalRecordService();
        this.consultationService = new ConsultationService(this.medicalRecordService);
        this.appointmentService = new MedicalAppointmentService();
        this.nurseService = new NurseService();
        this.invoiceService = new InvoiceService(this.patientService);

        // ✅ Inițializăm meniurile
        this.patientMenu = new PatientMenu(this.patientService, this.medicalRecordService, this.invoiceService);
        this.doctorMenu = new DoctorMenu(this.doctorService);
        this.consultationMenu = new ConsultationMenu(this.consultationService, this.patientService, this.doctorService, this.diagnosisService);
        this.appointmentMenu = new MedicalAppointmentMenu(this.appointmentService, this.doctorService, this.patientService, this.roomService);
        this.departmentMenu = new MedicalDepartmentMenu(this.departmentService, this.roomService, this.nurseService);
        this.invoiceMenu = new InvoiceMenu(this.invoiceService);
        this.nurseMenu = new NurseMenu(this.nurseService);
    }

    public static MainMenu getInstance() {
        if (instance == null) {
            instance = new MainMenu();
        }
        return instance;
    }

    public void init() {
        doctorService.loadFromDatabase();
        diagnosisService.loadFromDatabase(doctorService.getAllDoctors());
        medicalRecordService.loadMedicalRecordsFromDB(diagnosisService);
        patientService.linkMedicalRecords(medicalRecordService);

        System.out.println("✅ Datele au fost încărcate din baza de date.");
    }

    public void show() {
        int option;
        do {
            System.out.println("\n=== MENIU PRINCIPAL ===");
            System.out.println("1. Pacienți");
            System.out.println("2. Doctori");
            System.out.println("3. Consultații");
            System.out.println("4. Programări medicale");
            System.out.println("5. Facturi");
            System.out.println("6. Departamente medicale");
            System.out.println("7. Asistente medicale");
            System.out.println("0. Ieșire");
            System.out.print("Alegere: ");

            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }

            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> patientMenu.showMenu();
                case 2 -> doctorMenu.showMenu();
                case 3 -> consultationMenu.showMenu();
                case 4 -> appointmentMenu.showMenu();
                case 5 -> invoiceMenu.showMenu();
                case 6 -> departmentMenu.showMenu();
                case 7 -> nurseMenu.showMenu();
                case 0 -> System.out.println("🔚 Ieșire din aplicație.");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }

        } while (option != 0);
    }
}
