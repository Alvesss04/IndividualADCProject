package pt.unl.fct.di.adc.individualapp.util;

public class CreateAccountData {


    public String username;
    public String password;
    public String confirmation;
    public String phone;
    public String address;
    public String role;

    public CreateAccountData() {}

    public boolean isValid() {
        return isFilledIn(username) &&
                isFilledIn(password) &&
                isFilledIn(confirmation) &&
                isFilledIn(phone) &&
                isFilledIn(address) &&
                isFilledIn(role) &&
                username.contains("@") &&
                password.equals(confirmation) &&
                isValidRole(role);
    }

    private boolean isFilledIn(String field) {
        return field != null && !field.isBlank();
    }

    private boolean isValidRole(String role) {
        try {
            Role.valueOf(role.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}