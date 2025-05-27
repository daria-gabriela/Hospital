package main.java.hospital;

import main.java.hospital.model.Diagnosis;
import main.java.hospital.model.Doctor;
import main.java.hospital.service.DiagnosisService;
import main.java.hospital.service.DoctorService;

import java.util.List;

public class DiagnosisDBTest {
    public static void main(String[] args) {
        DoctorService doctorService = new DoctorService();
        doctorService.loadFromDatabase();

        DiagnosisService diagnosisService = new DiagnosisService();
        diagnosisService.loadFromDatabase(doctorService.getAllDoctors());

        List<Diagnosis> list = diagnosisService.getAllDiagnoses();

        System.out.println("=== DIAGNOSTICE ÎNCĂRCATE DIN DB ===");
        if (list.isEmpty()) {
            System.out.println("❌ NU s-a încărcat niciun diagnostic din DB.");
        } else {
            for (Diagnosis d : list) {
                System.out.println("✔️ " + d.getName() + " [medRecId: " + d.getMedicalRecordId() + "]");
            }
        }
    }
}
