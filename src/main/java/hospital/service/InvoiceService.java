package main.java.hospital.service;

import main.java.hospital.model.Invoice;
import main.java.hospital.model.Patient;
import main.java.hospital.util.AuditService;
import main.java.hospital.util.DatabaseConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class InvoiceService {
    private final List<Invoice> invoices = new ArrayList<>();
    private final PatientService patientService;

    public InvoiceService(PatientService patientService) {
        this.patientService = patientService;
        loadInvoicesFromDB();
    }

    public Invoice addInvoice(Patient patient, double amount, String description, LocalDate date, boolean isPaid) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO invoices (patient_id, amount, description, date, is_paid) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, patient.getId());
            stmt.setDouble(2, amount);
            stmt.setString(3, description);
            stmt.setDate(4, Date.valueOf(date));
            stmt.setBoolean(5, isPaid);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                Invoice invoice = new Invoice(id, patient, amount, description, date, isPaid);
                invoices.add(invoice);
                AuditService.getInstance().log("CREATE_INVOICE: ID=" + id);
                return invoice;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la adăugare factură: " + e.getMessage());
        }
        return null;
    }

    public void loadInvoicesFromDB() {
        invoices.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM invoices";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                int patientId = rs.getInt("patient_id");
                double amount = rs.getDouble("amount");
                String desc = rs.getString("description");
                LocalDate date = rs.getDate("date").toLocalDate();
                boolean isPaid = rs.getBoolean("is_paid");

                Optional<Patient> patientOpt = patientService.getPatientById(patientId);
                patientOpt.ifPresent(p -> {
                    Invoice invoice = new Invoice(id, p, amount, desc, date, isPaid);
                    invoices.add(invoice);
                });
            }
            AuditService.getInstance().log("LOAD_INVOICES_FROM_DB");
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la încărcare facturi: " + e.getMessage());
        }
    }

    public List<Invoice> getInvoicesForPatient(Patient patient) {
        List<Invoice> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM invoices WHERE patient_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patient.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Invoice invoice = new Invoice(
                        rs.getInt("id"),
                        patient,
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        rs.getDate("date").toLocalDate(),
                        rs.getBoolean("is_paid")
                );
                result.add(invoice);
            }
            AuditService.getInstance().log("READ_INVOICES_FOR_PATIENT: ID=" + patient.getId());
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la getInvoicesForPatient: " + e.getMessage());
        }
        return result;
    }

    public boolean markInvoiceAsPaid(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE invoices SET is_paid = TRUE WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                invoices.stream().filter(i -> i.getInvoiceId() == id).findFirst().ifPresent(Invoice::markAsPaid);
                AuditService.getInstance().log("MARK_INVOICE_PAID: ID=" + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la plată factură: " + e.getMessage());
        }
        AuditService.getInstance().log("MARK_INVOICE_PAID_FAILED: ID=" + id);
        return false;
    }

    public boolean deleteInvoiceById(int id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM invoices WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            int result = stmt.executeUpdate();
            if (result > 0) {
                invoices.removeIf(i -> i.getInvoiceId() == id);
                AuditService.getInstance().log("DELETE_INVOICE: ID=" + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la ștergere factură: " + e.getMessage());
        }
        AuditService.getInstance().log("DELETE_INVOICE_FAILED: ID=" + id);
        return false;
    }

    public Invoice createInvoiceForPatient(String cnp, double amount, String description, LocalDate date, boolean isPaid) {
        Optional<Patient> optional = patientService.getPatientByCnp(cnp);
        return optional.map(patient -> addInvoice(patient, amount, description, date, isPaid)).orElse(null);
    }

    public List<Invoice> getUnpaidInvoices() {
        return invoices.stream().filter(i -> !i.isPaid()).toList();
    }

    public List<Invoice> getPaidInvoices() {
        return invoices.stream().filter(Invoice::isPaid).toList();
    }

    public double getTotalRevenue() {
        return invoices.stream().mapToDouble(Invoice::getAmount).sum();
    }

    public double getTotalUnpaidAmount() {
        return invoices.stream().filter(i -> !i.isPaid()).mapToDouble(Invoice::getAmount).sum();
    }

    public void printAllInvoices() {
        if (invoices.isEmpty()) System.out.println("📄 Nu există facturi înregistrate.");
        else invoices.forEach(System.out::println);
        AuditService.getInstance().log("DISPLAY_ALL_INVOICES");
    }

    public void printUnpaidInvoices() {
        List<Invoice> list = getUnpaidInvoices();
        if (list.isEmpty()) System.out.println("✅ Toate facturile au fost achitate.");
        else list.forEach(System.out::println);
        AuditService.getInstance().log("DISPLAY_UNPAID_INVOICES");
    }

    public void printInvoicesForPatient(String cnp) {
        Optional<Patient> optional = patientService.getPatientByCnp(cnp);
        if (optional.isPresent()) {
            List<Invoice> list = getInvoicesForPatient(optional.get());
            if (list.isEmpty()) System.out.println("📭 Nicio factură pentru pacientul cu CNP: " + cnp);
            else list.forEach(System.out::println);
        } else {
            System.out.println("❌ Pacientul cu CNP " + cnp + " nu a fost găsit.");
        }
        AuditService.getInstance().log("DISPLAY_INVOICES_FOR_PATIENT: " + cnp);
    }
    public void menuInvoicesByDate() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("📅 Introdu data (yyyy-MM-dd): ");
            LocalDate dateInput = LocalDate.parse(scanner.nextLine());

            String sql = "SELECT * FROM invoices WHERE date = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDate(1, Date.valueOf(dateInput));
            ResultSet rs = stmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                int id = rs.getInt("id");
                int patientId = rs.getInt("patient_id");
                double amount = rs.getDouble("amount");
                String desc = rs.getString("description");
                LocalDate invoiceDate = rs.getDate("date").toLocalDate();
                boolean isPaid = rs.getBoolean("is_paid");
                Optional<Patient> patientOpt = patientService.getPatientById(patientId);
                if (patientOpt.isPresent()) {
                    Invoice invoice = new Invoice(id, patientOpt.get(), amount, desc, invoiceDate, isPaid);
                    System.out.println(invoice);
                    found = true;
                }
            }
            if (!found) System.out.println("📭 Nicio factură găsită pentru data specificată.");
            AuditService.getInstance().log("DISPLAY_INVOICES_BY_DATE: " + dateInput);
        } catch (Exception e) {
            System.out.println("❌ Eroare la interogare după dată: " + e.getMessage());
        }
    }

    public void menuInvoicesBetweenDates() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("📅 Introdu data de început (yyyy-MM-dd): ");
            LocalDate start = LocalDate.parse(scanner.nextLine());
            System.out.print("📅 Introdu data de sfârșit (yyyy-MM-dd): ");
            LocalDate end = LocalDate.parse(scanner.nextLine());

            String sql = "SELECT * FROM invoices WHERE date BETWEEN ? AND ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            ResultSet rs = stmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                int id = rs.getInt("id");
                int patientId = rs.getInt("patient_id");
                double amount = rs.getDouble("amount");
                String desc = rs.getString("description");
                LocalDate invoiceDate = rs.getDate("date").toLocalDate();
                boolean isPaid = rs.getBoolean("is_paid");
                Optional<Patient> patientOpt = patientService.getPatientById(patientId);
                if (patientOpt.isPresent()) {
                    Invoice invoice = new Invoice(id, patientOpt.get(), amount, desc, invoiceDate, isPaid);
                    System.out.println(invoice);
                    found = true;
                }
            }
            if (!found) System.out.println("📭 Nicio factură în perioada specificată.");
            AuditService.getInstance().log("DISPLAY_INVOICES_BETWEEN: " + start + " - " + end);
        } catch (Exception e) {
            System.out.println("❌ Eroare la interogare între date: " + e.getMessage());
        }
    }

    public List<Invoice> getInvoicesBetween(LocalDate start, LocalDate end) {
        List<Invoice> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM invoices WHERE date BETWEEN ? AND ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                int patientId = rs.getInt("patient_id");
                double amount = rs.getDouble("amount");
                String desc = rs.getString("description");
                LocalDate invoiceDate = rs.getDate("date").toLocalDate();
                boolean isPaid = rs.getBoolean("is_paid");

                Optional<Patient> patientOpt = patientService.getPatientById(patientId);
                patientOpt.ifPresent(p -> {
                    Invoice invoice = new Invoice(id, p, amount, desc, invoiceDate, isPaid);
                    result.add(invoice);
                });
            }
            AuditService.getInstance().log("GET_INVOICES_BETWEEN: " + start + " - " + end);
        } catch (SQLException e) {
            System.err.println("❌ Eroare JDBC la getInvoicesBetween: " + e.getMessage());
        }
        return result;
    }

    public double getTotalPaidAmount() {
        return invoices.stream().filter(Invoice::isPaid).mapToDouble(Invoice::getAmount).sum();
    }
}
