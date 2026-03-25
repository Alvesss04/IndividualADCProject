package pt.unl.fct.di.adc.individualapp.util;

public class CreateAccountData {

    public String username;
    public String password;
    public String confirmation;
    public String email;
    public String phone;
    public String address;
    public String role;

    public CreateAccountData() {} // empty constructor required by Jersey to parse JSON

    public boolean isValid() {
        return isFilledIn(username) &&
                isFilledIn(password) &&
                isFilledIn(confirmation) &&
                isFilledIn(email) &&
                isFilledIn(phone) &&
                isFilledIn(address) &&
                isFilledIn(role) &&
                email.contains("@") &&
                password.equals(confirmation) &&
                isValidRole(role);
    }

    //Returns true if a string is not null and not blank
    private boolean isFilledIn(String field) {
        return field != null && !field.isBlank();
    }

    // helper: only USER, BOFFICER or ADMIN are valid roles
    private boolean isValidRole(String role) {
        return role.equals("USER") || role.equals("BOFFICER") || role.equals("ADMIN");
    }
}