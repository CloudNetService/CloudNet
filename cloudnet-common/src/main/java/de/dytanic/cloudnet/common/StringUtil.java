package de.dytanic.cloudnet.common;

import java.util.Random;

/**
 * Includes string operations that are needed. Within the project and which are needed more frequently
 */
public final class StringUtil {


    /**
     * A char array of all letters from A to Z and 1 to 9
     */
    public static final char[] DEFAULT_ALPHABET_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private StringUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Generates a random string with a array of chars
     *
     * @param length the length of the generated string
     * @return the string, which was build with all random chars
     */
    public static String generateRandomString(int length) {
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();

        synchronized (random) {
            for (int i = 0; i < length; i++)
                stringBuilder.append(DEFAULT_ALPHABET_UPPERCASE[random.nextInt(DEFAULT_ALPHABET_UPPERCASE.length)]);
        }

        return stringBuilder.toString();
    }
}