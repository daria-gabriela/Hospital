package main.java.hospital.model;

/**
 * Enum care definește tipurile de tură pentru o asistentă medicală.
 * Include etichetă pentru afișare clară.
 */
public enum Shift {
    DAY("Tura de zi"),
    NIGHT("Tura de noapte");

    private final String label;

    Shift(String label) {
        this.label = label;
    }

    /**
     * Eticheta pentru afișare (ex: "Tura de zi").
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Caută o tură după nume (ex: "day", "nIghT").
     */
    public static Shift fromString(String value) {
        for (Shift s : Shift.values()) {
            if (s.name().equalsIgnoreCase(value) || s.label.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Tura invalidă: " + value);
    }
}
