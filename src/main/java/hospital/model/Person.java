package main.java.hospital.model;

/**
 * Clasă abstractă ce reprezintă o persoană generică în cadrul unui spital.
 */
public abstract class Person {
    private static int nextId = 1;

    protected  int id;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String phoneNumber;
    public Person(String firstname,String lastname)
    {
        this.firstName=firstname;
        this.lastName=lastname;
        this.id = nextId++;

    }
    public Person(String firstName, String lastName, String email, String phoneNumber) {
        this.id = nextId++;
        this.firstName = firstName != null ? firstName : "";
        this.lastName = lastName != null ? lastName : "";
        this.email = email;
        setPhoneNumber(phoneNumber); // folosim setter-ul pentru validare
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName : "";
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Setează numărul de telefon doar dacă este valid:
     * - 10 caractere
     * - doar cifre
     * - începe cu "07"
     *
     * @param phoneNumber numărul de telefon de validat
     * @throws IllegalArgumentException dacă numărul este invalid
     */
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("^07\\d{8}$")) {
            throw new IllegalArgumentException("Numărul de telefon trebuie să conțină exact 10 cifre și să înceapă cu '07'.");
        }
        this.phoneNumber = phoneNumber;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "Person {" +
                "ID: " + id +
                ", Nume: " + getFullName() +
                ", Email: " + (email != null ? email : "N/A") +
                ", Telefon: " + (phoneNumber != null ? phoneNumber : "N/A") +
                '}';
    }
}
