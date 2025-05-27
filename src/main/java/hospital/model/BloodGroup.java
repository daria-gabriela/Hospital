package main.java.hospital.model;

/**
 * Enum care definește grupele sanguine.
 */
public enum BloodGroup {
    A("A"),
    B("B"),
    AB("AB"),
    O("O");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Returnează grupa sanguină corespunzătoare unui string (case-insensitive).
     */
    public static BloodGroup fromString(String value) {
        for (BloodGroup bg : values()) {
            if (bg.label.equalsIgnoreCase(value)) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Grupă sanguină invalidă: " + value);
    }
}
