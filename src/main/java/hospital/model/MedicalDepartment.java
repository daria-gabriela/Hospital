package main.java.hospital.model;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.atomic.AtomicInteger;

public class MedicalDepartment {
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    private int id;
    private String name;
    private String floor;
    private String description;

    private final List<SimpleEntry<Doctor, List<Nurse>>> doctorNurseMap = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Nurse> allNurses = new ArrayList<>();

    public MedicalDepartment(String name, String floor, String description) {
        this.id = idCounter.getAndIncrement();
        this.name = name;
        this.floor = floor;
        this.description = description;
    }

    public MedicalDepartment(int id, String name, String floor, String description) {
        this.id = id;
        this.name = name;
        this.floor = floor;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public void addDoctor(Doctor doc) {
        doctorNurseMap.add(new SimpleEntry<>(doc, new ArrayList<>()));
    }

    public boolean removeDoctor(Doctor doc) {
        return doctorNurseMap.removeIf(e -> e.getKey().equals(doc));
    }

    public List<Doctor> getDoctors() {
        List<Doctor> list = new ArrayList<>();
        for (SimpleEntry<Doctor, List<Nurse>> e : doctorNurseMap) {
            list.add(e.getKey());
        }
        return list;
    }

    public List<Nurse> getNursesForDoctor(Doctor doc) {
        for (SimpleEntry<Doctor, List<Nurse>> e : doctorNurseMap) {
            if (e.getKey().equals(doc)) return e.getValue();
        }
        return new ArrayList<>();
    }

    public boolean addNurseToDoctor(Doctor doc, Nurse nurse) {
        for (SimpleEntry<Doctor, List<Nurse>> e : doctorNurseMap) {
            if (e.getKey().equals(doc)) {
                e.getValue().add(nurse);
                return true;
            }
        }
        return false;
    }

    public boolean removeNurseFromDoctor(Doctor doc, Nurse nurse) {
        for (SimpleEntry<Doctor, List<Nurse>> e : doctorNurseMap) {
            if (e.getKey().equals(doc)) {
                return e.getValue().remove(nurse);
            }
        }
        return false;
    }

    public boolean addNurse(Nurse nurse) {
        if (!allNurses.contains(nurse)) {
            allNurses.add(nurse);
            return true;
        }
        return false;
    }

    public boolean removeNurse(Nurse nurse) {
        return allNurses.remove(nurse);
    }

    public List<Nurse> getNurses() {
        return new ArrayList<>(allNurses);
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms);
    }

    public void addRoom(Room room) {
        if (room != null && room.getDepartment().equals(this)) {
            rooms.add(room);
        }
    }

    public boolean removeRoom(int roomNumber) {
        return rooms.removeIf(r -> r.getRoomNumber() == roomNumber);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dept {ID=").append(id)
                .append(", Nume='").append(name)
                .append("', Etaj='").append(floor);

        sb.append("'}\n Camere:\n");
        for (Room room : rooms) {
            sb.append("  🛏️ Camera ").append(room.getRoomNumber())
                    .append(" - ").append(room.getType())
                    .append(" - ").append(room.isOccupied() ? "ocupată" : "liberă").append("\n");
        }


        sb.append("Doctori și asistente:\n");

        Set<Nurse> nursesMentioned = new HashSet<>();

        for (Map.Entry<Doctor, List<Nurse>> entry : doctorNurseMap) {
            Doctor doc = entry.getKey();
            List<Nurse> nurses = entry.getValue();

            sb.append("  👨‍⚕️ ").append(doc.getFullName())
                    .append(" (cu ").append(nurses.size()).append(" asistente):\n");

            for (Nurse nurse : nurses) {
                if (nurse != null) {
                    sb.append("    👩‍⚕️ ").append(nurse.getFullName()).append("\n");
                    nursesMentioned.add(nurse);
                } else {
                    sb.append("    ⚠️ Asistentă null\n");
                }
            }
        }

        // Afișăm și asistentele generale neasociate cu un doctor
        List<Nurse> unassigned = new ArrayList<>(allNurses);
        unassigned.removeAll(nursesMentioned);

        if (!unassigned.isEmpty()) {
            sb.append("Asistente neasociate unui doctor:\n");
            for (Nurse nurse : unassigned) {
                if (nurse != null) {
                    sb.append("    👩‍⚕️ ").append(nurse.getFullName()).append("\n");
                }
            }
        }

        return sb.toString();
    }




}
