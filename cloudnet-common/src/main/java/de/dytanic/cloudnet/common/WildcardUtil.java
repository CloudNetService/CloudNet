package de.dytanic.cloudnet.common;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WildcardUtil {

    private WildcardUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T extends INameable> Collection<T> filterWildcard(Collection<T> inputValues, String regex) {
        return filterWildcard(inputValues, regex, true);
    }

    public static boolean anyMatch(Collection<? extends INameable> values, String regex) {
        return anyMatch(values, regex, true);
    }

    public static <T extends INameable> Collection<T> filterWildcard(Collection<T> inputValues, String regex, boolean caseSensitive) {
        Pattern pattern = prepare(regex, caseSensitive);
        return inputValues.stream()
                .filter(t -> pattern.matcher(t.getName()).matches())
                .collect(Collectors.toList());
    }

    public static boolean anyMatch(Collection<? extends INameable> values, String regex, boolean caseSensitive) {
        Pattern pattern = prepare(regex, caseSensitive);
        return values.stream()
                .anyMatch(t -> pattern.matcher(t.getName()).matches());
    }

    private static Pattern prepare(String regex, boolean caseSensitive) {
        regex = regex.replace("*", "(.*)");
        return caseSensitive ? Pattern.compile(regex) : Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

}
