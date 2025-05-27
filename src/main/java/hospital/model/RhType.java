package main.java.hospital.model;

/**
 * Enum care definește tipul RH: pozitiv sau negativ.
 */
public enum RhType {
    POSITIVE("+"),
    NEGATIVE("-");

    private final String symbol;

    RhType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }

    /**
     * Returnează tipul RH corespunzător unui string (ex: "+" -> POSITIVE).
     */
    public static RhType fromString(String value) {
        switch (value.trim()) {
            case "+":
            case "POSITIVE":
            case "positive":
                return POSITIVE;
            case "-":
            case "NEGATIVE":
            case "negative":
                return NEGATIVE;
            default:
                throw new IllegalArgumentException("RH invalid: " + value);
        }
    }
}
