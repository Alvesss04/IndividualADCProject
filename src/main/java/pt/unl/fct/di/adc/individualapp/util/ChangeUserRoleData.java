package pt.unl.fct.di.adc.individualapp.util;

public class ChangeUserRoleData {

    public String username;
    public String newRole;

    public ChangeUserRoleData() {}

    public boolean isValid() {
        return isFilledIn(username) &&
                isFilledIn(newRole) &&
                username.contains("@") &&
                isValidRole(newRole);
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