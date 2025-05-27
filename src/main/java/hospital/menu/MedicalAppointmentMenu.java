package main.java.hospital.menu;

import main.java.hospital.model.*;
import main.java.hospital.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class MedicalAppointmentMenu {

    private final MedicalAppointmentService appointmentService;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final RoomService roomService;
    private final Scanner scanner;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MedicalAppointmentMenu(MedicalAppointmentService appointmentService,
                                  DoctorService doctorService,
                                  PatientService patientService,
                                  RoomService roomService) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.roomService = roomService;
        this.scanner = new Scanner(System.in);
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU PROGRAMĂRI ===");
            System.out.println("1. Adaugă programare");
            System.out.println("2. Șterge programare");
            System.out.println("3. Afișează toate programările");
            System.out.println("4. Afișează programările unui pacient");
            System.out.println("5. Verifică disponibilitate doctor");
            System.out.println("6. Afișează programările dintr-o zi");
            System.out.println("7. Actualizează o programare");
            System.out.println("0. Înapoi");
            System.out.print("Alegere: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> addAppointment();
                case 2 -> deleteAppointment();
                case 3 -> appointmentService.printAllAppointments();
                case 4 -> showAppointmentsForPatient();
                case 5 -> checkDoctorAvailability();
                case 6 -> showAppointmentsByDate();
                case 7 -> updateAppointment();
                case 0 -> System.out.println("Revenire...");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    /* ========== ADD ========== */
    private void addAppointment() {

        /* --- pacient --- */
        Patient patient;
        while (true) {
            System.out.print("CNP pacient: ");
            String cnp = scanner.nextLine();
            if (cnp.equalsIgnoreCase("exit")) return;
            if (!cnp.matches("\\d{13}")) {
                System.out.println("❌ CNP invalid. Încearcă din nou sau 'exit'.");
                continue;
            }
            Optional<Patient> opt = patientService.getPatientByCnp(cnp);
            if (opt.isEmpty()) {
                System.out.println("❌ Pacient inexistent. Încearcă din nou sau 'exit'.");
                continue;
            }
            patient = opt.get();
            break;
        }

        /* --- doctor --- */
        Doctor doctor;
        while (true) {
            System.out.print("Cod parafă doctor: ");
            String code = scanner.nextLine();
            if (code.equalsIgnoreCase("exit")) return;
            if (!code.matches("^[A-Z0-9]{3,10}$")) {
                System.out.println("❌ Cod parafă invalid. Încearcă din nou sau 'exit'.");
                continue;
            }
            Optional<Doctor> opt = doctorService.getDoctorByParafaCode(code);
            if (opt.isEmpty()) {
                System.out.println("❌ Doctor inexistent. Încearcă din nou sau 'exit'.");
                continue;
            }
            doctor = opt.get();
            break;
        }

        /* --- dată & oră --- */
        LocalDateTime dateTime;
        while (true) {
            System.out.print("Dată & oră (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) return;
            try {
                dateTime = LocalDateTime.parse(input, DT_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("❌ Format invalid. Exemplu: 2025-06-01 14:30 sau 'exit'.");
                continue;
            }
            if (!appointmentService.isDoctorAvailable(doctor.getId(), dateTime)) {
                System.out.println("⚠️ Doctorul este deja ocupat atunci. Alege alt interval sau 'exit'.");
                continue;
            }
            break;
        }

        /* --- cameră --- */
        Room room;
        while (true) {
            System.out.print("Număr cameră: ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) return;
            try {
                int nr = Integer.parseInt(input);
                Optional<Room> opt = roomService.getRoomByNumber(nr);
                if (opt.isEmpty()) {
                    System.out.println("❌ Cameră inexistentă. Reîncearcă sau 'exit'.");
                    continue;
                }
                room = opt.get();
                if (room.isOccupied()) {
                    System.out.println("⚠️ Cameră ocupată. Alege alta sau 'exit'.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("❌ Număr invalid. Reîncearcă sau 'exit'.");
            }
        }

        room.occupy(0);               // marcăm ocupată (se poate elibera când termini)

        MedicalAppointment appt = new MedicalAppointment(
                patient, doctor, dateTime, "Consultație", room
        );
        appointmentService.addAppointment(appt);
        System.out.println("✅ Programare adăugată cu ID: " + appt.getId());
    }

    /* ========== DELETE ========== */
    private void deleteAppointment() {
        System.out.print("ID programare: ");
        while (!scanner.hasNextInt()) {
            System.out.print("⚠️ Introdu un ID valid: ");
            scanner.next();
        }
        int id = scanner.nextInt();
        scanner.nextLine();

        if (appointmentService.deleteAppointment(id)) {
            System.out.println("🗑️ Programare ștearsă.");
        } else {
            System.out.println("❌ Programarea nu a fost găsită.");
        }
    }

    /* ========== LIST BY PATIENT ========== */
    private void showAppointmentsForPatient() {
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();
        Optional<Patient> opt = patientService.getPatientByCnp(cnp);
        if (opt.isEmpty()) {
            System.out.println("❌ Pacient absent.");
            return;
        }
        List<MedicalAppointment> list = appointmentService.getAppointmentsByPatientId(opt.get().getId());
        if (list.isEmpty()) {
            System.out.println("📭 Nicio programare.");
        } else list.forEach(System.out::println);
    }

    /* ========== CHECK AVAILABILITY ========== */
    private void checkDoctorAvailability() {
        System.out.print("Cod parafă: ");
        String code = scanner.nextLine();
        Optional<Doctor> opt = doctorService.getDoctorByParafaCode(code);
        if (opt.isEmpty()) {
            System.out.println("❌ Doctor inexistent.");
            return;
        }
        System.out.print("Dată & oră (yyyy-MM-dd HH:mm): ");
        try {
            LocalDateTime dt = LocalDateTime.parse(scanner.nextLine(), DT_FMT);
            System.out.println(appointmentService.isDoctorAvailable(opt.get().getId(), dt)
                    ? "✅ Disponibil" : "⛔ Ocupat");
        } catch (DateTimeParseException e) {
            System.out.println("❌ Format dată invalid.");
        }
    }

    /* ========== LIST BY DATE ========== */
    private void showAppointmentsByDate() {
        System.out.print("Dată (yyyy-MM-dd): ");
        try {
            LocalDate date = LocalDate.parse(scanner.nextLine());
            List<MedicalAppointment> list = appointmentService.getAppointmentsByDate(date.atStartOfDay());
            if (list.isEmpty()) System.out.println("📭 Nicio programare.");
            else list.forEach(System.out::println);
        } catch (DateTimeParseException e) {
            System.out.println("❌ Format dată invalid.");
        }
    }

    /* ========== UPDATE ========== */
    private void updateAppointment() {
        System.out.print("ID programare: ");
        while (!scanner.hasNextInt()) {
            System.out.print("⚠️ Introdu un ID valid: ");
            scanner.next();
        }
        int id = scanner.nextInt();
        scanner.nextLine();

        Optional<MedicalAppointment> opt = appointmentService.getAppointmentById(id);
        if (opt.isEmpty()) {
            System.out.println("❌ Programarea nu există.");
            return;
        }

        int choice;
        do {
            System.out.println("\n--- Actualizare programare ID " + id + " ---");
            System.out.println("1. Dată & oră");
            System.out.println("2. Cameră");
            System.out.println("3. Observații");
            System.out.println("0. Înapoi");
            System.out.print("Alegere: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> updateDateTime(id);
                case 2 -> updateRoom(id);
                case 3 -> {
                    System.out.print("Noi observații: ");
                    appointmentService.updateAppointmentNotes(id, scanner.nextLine());
                }
                case 0 -> System.out.println("Revenire...");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (choice != 0);
    }

    private void updateDateTime(int id) {
        while (true) {
            System.out.print("Nouă dată & oră (yyyy-MM-dd HH:mm): ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) return;
            try {
                LocalDateTime dt = LocalDateTime.parse(input, DT_FMT);
                appointmentService.updateAppointmentDateTime(id, dt);
                System.out.println("✅ Dată actualizată.");
                break;
            } catch (DateTimeParseException e) {
                System.out.println("❌ Format invalid. Reîncearcă sau 'exit'.");
            }
        }
    }

    private void updateRoom(int id) {
        while (true) {
            System.out.print("Număr cameră nou: ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) return;
            try {
                int nr = Integer.parseInt(input);
                Optional<Room> opt = roomService.getRoomByNumber(nr);
                if (opt.isEmpty()) {
                    System.out.println("❌ Cameră inexistentă. Reîncearcă sau 'exit'.");
                    continue;
                }
                appointmentService.updateAppointmentRoom(id, opt.get());
                System.out.println("✅ Cameră actualizată.");
                break;
            } catch (NumberFormatException e) {
                System.out.println("❌ Număr invalid. Reîncearcă sau 'exit'.");
            }
        }
    }
}
