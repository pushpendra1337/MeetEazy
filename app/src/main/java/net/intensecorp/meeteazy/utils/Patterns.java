package net.intensecorp.meeteazy.utils;

import java.util.Random;
import java.util.regex.Pattern;

public class Patterns {

    public static final Pattern FIRST_NAME_PATTERN = Pattern.compile("^\\p{L}{1,50}");

    public static final Pattern LAST_NAME_PATTERN = Pattern.compile("^([\\p{L}]{1,50}.?'?-?[\\p{L}]{0,50})");

    public static final Pattern EMAIL_PATTERN = Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
            + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
            + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
            + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$");

    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[(@#$%^&+=])(?=\\S+$).{8,}$");

    private static final String ENGLISH_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String generateRoomId() {

        String generatedRoomId;
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        int length = 9;

        for (int j = 0; j < length; j++) {
            int index = random.nextInt(ENGLISH_ALPHABET.length());
            char randomChar = ENGLISH_ALPHABET.charAt(index);
            stringBuilder.append(randomChar);
        }

        generatedRoomId = stringBuilder.toString();

        return generatedRoomId;
    }
}
