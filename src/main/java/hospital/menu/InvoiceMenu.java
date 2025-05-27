package main.java.hospital.menu;

import main.java.hospital.model.Invoice;
import main.java.hospital.service.InvoiceService;
import main.java.hospital.util.AuditService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class InvoiceMenu {

    private final InvoiceService invoiceService;
    private final Scanner scanner;

    public InvoiceMenu(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
        this.scanner = new Scanner(System.in);
    }

    public void showMenu() {
        int option;
        do {
            System.out.println("\n=== MENIU FACTURI ===");
            System.out.println("1. Creează factură");
            System.out.println("2. Afișează toate facturile");
            System.out.println("3. Afișează facturile neplătite");
            System.out.println("4. Afișează facturile unui pacient");
            System.out.println("5. Marchează factură ca plătită");
            System.out.println("6. Șterge factură");
            System.out.println("7. Raport: Total venituri și restanțe");
            System.out.println("8. Afișează facturi între două date");
            System.out.println("9. Afișează facturi pentru un pacient între două date");
            System.out.println("0. Înapoi");
            System.out.print("Alegere: ");
            while (!scanner.hasNextInt()) {
                System.out.print("⚠️ Introdu un număr valid: ");
                scanner.next();
            }
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> createInvoice();
                case 2 -> invoiceService.printAllInvoices();
                case 3 -> invoiceService.printUnpaidInvoices();
                case 4 -> showInvoicesForPatient();
                case 5 -> markAsPaid();
                case 6 -> deleteInvoice();
                case 7 -> showReport();
                case 8 -> invoiceService.menuInvoicesBetweenDates();
                case 9 -> filterPatientInvoicesByDate();
                case 0 -> System.out.println("Revenire la meniul principal...");
                default -> System.out.println("⚠️ Opțiune invalidă.");
            }
        } while (option != 0);
    }

    private void createInvoice() {
        while (true) {
            try {
                System.out.print("CNP pacient: ");
                String cnp = scanner.nextLine();
                if (cnp.equalsIgnoreCase("exit")) return;
                if (!cnp.matches("\\d{13}")) {
                    System.out.println("❌ CNP invalid. Reîncearcă sau tastează 'exit' pentru a anula.");
                    continue;
                }

                double amount;
                while (true) {
                    System.out.print("Suma: ");
                    String input = scanner.nextLine();
                    if (input.equalsIgnoreCase("exit")) return;
                    try {
                        amount = Double.parseDouble(input);
                        if (amount < 0) throw new NumberFormatException();
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Suma invalidă. Introdu un număr pozitiv sau 'exit'.");
                    }
                }

                System.out.print("Descriere: ");
                String description = scanner.nextLine();
                if (description.equalsIgnoreCase("exit")) return;

                LocalDate date;
                while (true) {
                    System.out.print("Dată (yyyy-MM-dd): ");
                    String input = scanner.nextLine();
                    if (input.equalsIgnoreCase("exit")) return;
                    try {
                        date = LocalDate.parse(input);
                        break;
                    } catch (DateTimeParseException e) {
                        System.out.println("❌ Format dată invalid. Exemplu valid: 2025-01-01 sau tastează 'exit'.");
                    }
                }

                boolean isPaid;
                while (true) {
                    System.out.print("Factura este plătită? (true/false): ");
                    String input = scanner.nextLine();
                    if (input.equalsIgnoreCase("exit")) return;
                    if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")) {
                        isPaid = Boolean.parseBoolean(input);
                        break;
                    } else {
                        System.out.println("❌ Introdu 'true' sau 'false' sau tastează 'exit'.");
                    }
                }

                Invoice invoice = invoiceService.createInvoiceForPatient(cnp, amount, description, date, isPaid);
                if (invoice != null) {
                    System.out.println("✅ Factură creată cu ID: " + invoice.getInvoiceId());
                    AuditService.getInstance().log("CREATE_INVOICE: " + invoice.getInvoiceId());
                }
                break;
            } catch (Exception e) {
                System.out.println("❌ Eroare la crearea facturii: " + e.getMessage());
                break;
            }
        }
    }

    private void showInvoicesForPatient() {
        System.out.print("CNP pacient: ");
        String cnp = scanner.nextLine();
        invoiceService.printInvoicesForPatient(cnp);
        AuditService.getInstance().log("READ_INVOICES_PATIENT: " + cnp);
    }

    private void markAsPaid() {
        System.out.print("ID factură: ");
        while (!scanner.hasNextInt()) {
            System.out.print("⚠️ Introdu un ID valid: ");
            scanner.next();
        }
        int id = scanner.nextInt();
        scanner.nextLine();
        boolean success = invoiceService.markInvoiceAsPaid(id);
        if (success) {
            System.out.println("✅ Factura marcată ca plătită.");
            AuditService.getInstance().log("MARK_INVOICE_PAID: " + id);
        } else {
            System.out.println("❌ Factura nu a fost găsită sau era deja plătită.");
        }
    }

    private void deleteInvoice() {
        System.out.print("ID factură: ");
        while (!scanner.hasNextInt()) {
            System.out.print("⚠️ Introdu un ID valid: ");
            scanner.next();
        }
        int id = scanner.nextInt();
        scanner.nextLine();
        boolean success = invoiceService.deleteInvoiceById(id);
        if (success) {
            System.out.println("🗑️ Factura ștearsă.");
            AuditService.getInstance().log("DELETE_INVOICE: " + id);
        } else {
            System.out.println("❌ Factura nu a fost găsită.");
        }
    }

    private void showReport() {
        double total = invoiceService.getTotalRevenue();
        double paid = invoiceService.getTotalPaidAmount();
        double unpaid = invoiceService.getTotalUnpaidAmount();
        System.out.println("\n📊 Raport financiar:");
        System.out.printf("- Total încasări: %.2f lei%n", total);
        System.out.printf("- Total plătit: %.2f lei%n", paid);
        System.out.printf("- Total restanțe: %.2f lei%n", unpaid);
        AuditService.getInstance().log("INVOICE_REPORT");
    }

    private void filterPatientInvoicesByDate() {
        try {
            System.out.print("CNP pacient: ");
            String cnp = scanner.nextLine();

            System.out.print("Dată început (yyyy-MM-dd): ");
            LocalDate start = LocalDate.parse(scanner.nextLine());

            System.out.print("Dată sfârșit (yyyy-MM-dd): ");
            LocalDate end = LocalDate.parse(scanner.nextLine());

            List<Invoice> filtered = invoiceService.getInvoicesBetween(start, end);
            boolean found = false;
            for (Invoice invoice : filtered) {
                if (invoice.getPatient().getCnp().equalsIgnoreCase(cnp)) {
                    System.out.println(invoice);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("📭 Nicio factură găsită pentru acest pacient în intervalul specificat.");
            }

            AuditService.getInstance().log("FILTER_PATIENT_INVOICES_BETWEEN_DATES: " + cnp);
        } catch (Exception e) {
            System.out.println("❌ Eroare: format dată invalid sau date lipsă.");
        }
    }
}
