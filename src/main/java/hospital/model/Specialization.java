package main.java.hospital.model;

public enum Specialization {
    CARDIOLOGIE("Cardiologie", "Bolile inimii"),
    NEUROLOGIE("Neurologie", "Boli ale sistemului nervos"),
    ORL("ORL", "Afectiuni ale urechii, nasului si gatului"),
    PEDIATRIE("Pediatrie", "Sanatatea copiilor"),
    MEDICINA_INTERNA("Medicină Internă", "Diagnostic și tratament general al adulților"),
    DERMATOLOGIE("Dermatologie", "Afecțiuni ale pielii"),
    GINECOLOGIE("Ginecologie", "Sănătatea sistemului reproducător feminin"),
    UROLOGIE("Urologie", "Afecțiuni ale sistemului urinar"),
    PSIHIATRIE("Psihiatrie", "Tulburări mentale și emoționale"),
    OFTALMOLOGIE("Oftalmologie", "Afecțiuni ale ochilor"),
    ORTOPEDIE("Ortopedie", "Aparatul locomotor: oase, articulații, mușchi"),
    CHIRURGIE_GENERALA("Chirurgie Generală", "Intervenții chirurgicale non-specializate"),
    ANESTEZIOLOGIE("Anesteziologie", "Anestezie și terapie intensivă"),
    ENDOCRINOLOGIE("Endocrinologie", "Tulburări hormonale și ale glandelor endocrine"),
    GASTROENTEROLOGIE("Gastroenterologie", "Boli ale sistemului digestiv"),
    HEMATOLOGIE("Hematologie", "Boli ale sângelui și măduvei osoase"),
    INFECTIOASE("Boli Infecțioase", "Afecțiuni cauzate de agenți infecțioși"),
    NEFROLOGIE("Nefrologie", "Boli ale rinichilor"),
    REUMATOLOGIE("Reumatologie", "Boli articulare și autoimune"),
    ONCOLOGIE("Oncologie", "Tratamentul cancerului"),
    NEONATOLOGIE("Neonatologie", "Îngrijirea nou-născuților");

    private final String displayName;
    private final String description;

    Specialization(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName + " - " + description;
    }
}
