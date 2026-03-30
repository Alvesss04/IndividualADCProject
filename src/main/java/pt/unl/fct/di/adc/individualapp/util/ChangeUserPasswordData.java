package pt.unl.fct.di.adc.individualapp.util;

public class ChangeUserPasswordData {
    public String username;
    public String newPassword;
    public String oldPassword;

    public ChangeUserPasswordData(){}

    public boolean isValid() {
        return isFilledIn(username) && isFilledIn(oldPassword) && isFilledIn(newPassword) &&username.contains("@");
    }

    private boolean isFilledIn(String field) {
        return field != null && !field.isBlank();
    }
}
