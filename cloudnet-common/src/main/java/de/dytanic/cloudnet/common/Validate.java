package de.dytanic.cloudnet.common;

/**
 * The methods on this class can check some states or number validations of strings
 */
public final class Validate {

    private Validate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Proof ob the input argument is null and throws an NullPointerException when the value is null
     *
     * @param object the input argument which you check is null
     * @param <T>    the generic type of the input argument
     * @return the same instance which you add into the first parameter
     */
    public static <T> T checkNotNull(T object) {
        checkNotNull(object, null);

        return object;
    }

    /**
     * Proof ob the input argument is null and throws an NullPointerException when the value is null
     *
     * @param object  the input argument which you check is null
     * @param message The following message, which should output if the object is null
     * @param <T>     the generic type of the input argument
     * @return the same instance which you add into the first parameter
     */
    public static <T> T checkNotNull(T object, String message) {
        if (object == null)
            throw new NullPointerException(message == null ? "The input object is null. Please check the parameters!" : message);

        return object;
    }

    /**
     * Excepted that the following condition is true.
     * If is not it throws an IllegalArgumentException
     *
     * @param value the following condition which should checked
     * @return true
     */
    public static boolean assertTrue(boolean value) {
        assertTrue(value, null);

        return value;
    }

    /**
     * Excepted that the following condition is true.
     * If is not it throws an IllegalArgumentException
     *
     * @param value   the following condition which should checked
     * @param message the message which should throw, when the condition is false
     * @return true
     */
    public static boolean assertTrue(boolean value, String message) {
        if (!value)
            throw new IllegalArgumentException(message == null ? "input condition is false. Expected true" : message);

        return value;
    }

    /**
     * Excepted that the following condition is false.
     * If is not it throws an IllegalArgumentException
     *
     * @param value the following condition which should checked
     * @return false
     */
    public static boolean assertFalse(boolean value) {
        assertFalse(value, "value is true");

        return value;
    }

    /**
     * Excepted that the following condition is false.
     * If is not it throws an IllegalArgumentException
     *
     * @param value   the following condition which should checked
     * @param message the message which should throw, when the condition is true
     * @return false
     */
    public static boolean assertFalse(boolean value, String message) {
        if (value) throw new IllegalArgumentException(message);

        return value;
    }

    /**
     * Tests that the following input string can be parse to int or not
     * It invokes to test the Integer.parseInt() method
     *
     * @param input the following test string
     * @return true if the string can parse to int
     */
    public static boolean testStringParseToInt(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Tests that the following input string can be parse to double or not
     * It invokes to test the Double.parseDouble() method
     *
     * @param input the following test string
     * @return true if the string can parse to double
     */
    public static boolean testStringParseToDouble(String input) {
        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Tests that the following input string can be parse to long or not
     * It invokes to test the Long.parseLong() method
     *
     * @param input the following test string
     * @return true if the string can parse to long
     */
    public static boolean testStringParseToLong(String input) {
        try {
            Long.parseLong(input);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}