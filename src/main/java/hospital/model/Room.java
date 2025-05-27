package main.java.hospital.model;

public class Room {
    private  int roomNumber;
    private String type;
    private MedicalDepartment department;
    private boolean isOccupied;
    private Integer occupiedByAppointmentId;

    // Constructor complet recomandat
    public Room(int roomNumber, String type, MedicalDepartment department, boolean isOccupied) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.department = department;
        this.isOccupied = isOccupied;
        this.occupiedByAppointmentId = null;
    }

    // === Getteri ===
    public int getRoomNumber() {
        return roomNumber;
    }

    public String getType() {
        return type;
    }

    public MedicalDepartment getDepartment() {
        return department;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public Integer getOccupiedByAppointmentId() {
        return occupiedByAppointmentId;
    }

    // === Operații ocupare/eliberare ===
    public void occupy(int appointmentId) {
        this.isOccupied = true;
        this.occupiedByAppointmentId = appointmentId;
    }

    public void free() {
        this.isOccupied = false;
        this.occupiedByAppointmentId = null;
    }

    // === Setteri ===
    public void setType(String type) {
        if (type != null && !type.isBlank()) {
            this.type = type;
        }
    }

    public void setOccupied(boolean occupied) {
        this.isOccupied = occupied;
        if (!occupied) {
            this.occupiedByAppointmentId = null;
        }
    }

    public void setDepartment(MedicalDepartment newDepartment) {
        if (newDepartment != null) {
            this.department = newDepartment;
        }
    }

    @Override
    public String toString() {
        return "Room {" +
                "Nr: " + roomNumber +
                ", Tip: '" + type + '\'' +
                ", Dept: '" + (department != null ? department.getName() : "N/A") + '\'' +
                ", Ocupată: " + (isOccupied ? "DA (Appt " + occupiedByAppointmentId + ")" : "NU") +
                '}';
    }

    public void setRoomNumber(int generatedId) {

        this.roomNumber= generatedId;
    }
}
