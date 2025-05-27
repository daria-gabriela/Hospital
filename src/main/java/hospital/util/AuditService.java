package main.java.hospital.util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditService {
    private static AuditService instance;
    private static final String FILE_PATH = "audit\\audit_log.csv";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private AuditService() {
        try {
            java.io.File file = new java.io.File(FILE_PATH);
            java.io.File parentDir = file.getParentFile();

            // Creează folderul dacă nu există
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file, true)) {
                if (file.length() == 0) {
                    writer.write("actiune,timestamp\n");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Eroare inițializare AuditService: " + e.getMessage());
        }
    }

    public static AuditService getInstance() {
        if (instance == null) {
            instance = new AuditService();
        }
        return instance;
    }

    public void log(String action) {
        try (FileWriter writer = new FileWriter(FILE_PATH, true)) {
            writer.write(action + "," + FORMATTER.format(LocalDateTime.now()) + "\n");
        } catch (IOException e) {
            System.err.println("❌ Eroare la scrierea în audit_log.csv: " + e.getMessage());
        }
    }
}
