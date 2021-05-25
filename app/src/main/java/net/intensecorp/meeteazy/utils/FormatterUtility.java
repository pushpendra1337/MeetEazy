package net.intensecorp.meeteazy.utils;

public class FormatterUtility {

    private static final String FORMAT_FULL_NAME = "%s %s";

    public static String getFullName(String firstName, String lastName) {
        return String.format(FORMAT_FULL_NAME, firstName, lastName);
    }
}
