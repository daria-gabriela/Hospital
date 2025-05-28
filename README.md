# 🏥 Hospital Management System

**Hospital Management System** este o aplicație Java destinată gestionării activităților dintr-un spital. Acest sistem ajută la administrarea pacienților, doctorilor, asistentelor, programărilor medicale, camerelor, departamentelor, fișelor medicale, rețetelor și diagnosticelor.

## 📌 Etapa I – Definirea și Implementarea Sistemului

### 🔍 Acțiuni posibile în sistem (cel puțin 10):

1. Adăugarea unui pacient nou
2. Programarea unei consultații
3. Afișarea tuturor doctorilor disponibili
4. Actualizarea datelor unui pacient
5. Ștergerea unei programări
6. Atribuirea unei asistente unui doctor
7. Emiterea unei rețete medicale pentru un diagnostic
8. Căutarea tuturor consultațiilor unui pacient
9. Afișarea facturilor emise pentru un pacient
10. Sortarea doctorilor după experiență

### 🧱 Tipuri de obiecte (cel puțin 8):

* `Patient`
* `Doctor`
* `Nurse`
* `Room`
* `MedicalDepartment`
* `MedicalAppointment`
* `Diagnosis`
* `Prescription`
* `MedicalRecord`
* `Invoice`

### ✅ Cerințe de implementare acoperite:

* Clase simple Java cu atribute private/protected și metode getter/setter
* Utilizarea colecțiilor: `List`, `Map`, `TreeSet` (colecție sortată)
* Moștenire (ex. `MedicalStaff` extins de `Doctor` și `Nurse`)
* Servicii dedicate pentru fiecare entitate (`PatientService`, `DoctorService`, etc.)
* Clasa `Main` care gestionează meniul principal și apelează metodele din servicii

---

## 📌 Etapa II – Persistență și Funcționalități Avansate

### 💾 Persistența datelor

* S-a folosit o bază de date relațională MySQL
* Persistența este realizată prin JDBC
* S-au implementat operații CRUD pentru cel puțin 4 clase:

  * `PatientService`
  * `DoctorService`
  * `MedicalRecordService`
  * `PrescriptionService`

### 🧩 Servicii Singleton Generice

* Toate serviciile principale sunt implementate folosind modelul Singleton
* Asigură acces controlat și consistent la baza de date

### 🧾 Serviciu de Audit

* Acțiunile efectuate în sistem sunt logate într-un fișier CSV
* Structură: `nume_actiune, timestamp`
* Exemple de acțiuni logate:

  * `ADD_PATIENT`, `CREATE_APPOINTMENT`, `ISSUE_PRESCRIPTION`

---

## ⚙️ Tehnologii Utilizate

* **Java 17**
* **MySQL**
* **JDBC**
* **IntelliJ IDEA**
* **GitHub**

---

## 🚀 Funcționalități

* Gestionarea completă a pacienților, doctorilor și asistentelor
* Programări medicale și asocierea acestora cu fișe și diagnostic
* Emiterea rețetelor și facturilor
* Interfață tip consolă ușor de utilizat
* Persistență în baza de date + logare audit

---

## 🛠️ Configurare rapidă

```bash
git clone https://github.com/daria-gabriela/Hospital.git
cd Hospital
```

1. Creează baza de date `hospital_db` în MySQL.
2. Importează scriptul SQL cu toate tabelele.
3. Adaugă `mysql-connector-j-9.3.0.jar` la claspath.
4. Rulează `Main.java` din IntelliJ IDEA.


