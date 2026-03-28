package pt.unl.fct.di.adc.individualapp.util;

public class ModifyAccountData {

    public String username;
    public Attributes attributes;

    public static class Attributes {
        public String phone;
        public String address;
    }

    public ModifyAccountData() {}

    public boolean isValid() {
        if (!isFilledIn(username) || !username.contains("@")) return false;

        if (attributes == null) return false;

        boolean hasPhone   = isFilledIn(attributes.phone);
        boolean hasAddress = isFilledIn(attributes.address);

        return hasPhone || hasAddress;
    }

    private boolean isFilledIn(String field) {
        return field != null && !field.isBlank();
    }
}