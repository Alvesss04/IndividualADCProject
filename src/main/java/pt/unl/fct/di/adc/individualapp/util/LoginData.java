package pt.unl.fct.di.adc.individualapp.util;

public class LoginData {

    public String username;
    public String password;

    public LoginData() {} // empty constructor required by Jersey

    // checks if all required fields are present and valid
    public boolean isValid() {
        return isFilledIn(username) &&
                isFilledIn(password) &&
                username.contains("@"); // username must be email format
    }

    private boolean isFilledIn(String field) {
        return field != null && !field.isBlank();
    }
}