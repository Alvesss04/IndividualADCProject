package pt.unl.fct.di.adc.individualapp.util;

public class CreateAccountData {

    // note: no email field anymore! username IS the email (e.g. john@adc.pt)
    public String username;
    public String password;
    public String confirmation;
    public String phone;
    public String address;
    public String role;

    public CreateAccountData() {} // empty constructor required by Jersey

    // checks if all required fields are present and valid
    public boolean isValid() {
        return isFilledIn(username) &&
                isFilledIn(password) &&
                isFilledIn(confirmation) &&
                isFilledIn(phone) &&
                isFilledIn(address) &&
                isFilledIn(role) &&
                username.contains("@") &&       // username must be email format
                password.equals(confirmation) && // passwords must match
                isValidRole(role);
    }

    // helper: returns true if a string is not null and not blank
    private boolean isFilledIn(String field) {
        return field != null && !field.isBlank();
    }

    // helper: checks role against our Role enum
    private boolean isValidRole(String role) {
        try {
            Role.valueOf(role.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false; // not a valid role
        }
    }
}