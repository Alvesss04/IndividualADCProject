package pt.unl.fct.di.adc.individualapp.util;

public class LoginData {

    public String username;
    public String password;

    public LoginData() {}

    public boolean isValid() {
        return isFilledIn(username) &&
                isFilledIn(password) &&
                username.contains("@");
    }

    private boolean isFilledIn(String field) {
        return field != null && !field.isBlank();
    }
}