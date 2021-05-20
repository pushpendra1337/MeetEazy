package net.intensecorp.meeteazy.utils;

import java.util.regex.Pattern;

public class RegExUtility {

    public static final Pattern FIRST_NAME_PATTERN = Pattern.compile("^\\p{L}{1,50}");

    public static final Pattern LAST_NAME_PATTERN = Pattern.compile("^([\\p{L}]{1,50}.?'?-?[\\p{L}]{0,50})");

    public static final Pattern EMAIL_PATTERN = Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
            + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
            + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
            + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$");

    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[(@#$%^&+=])(?=\\S+$).{8,}$");
}
