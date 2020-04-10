package de.dytanic.cloudnet.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class WildcardUtil {

    private WildcardUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T extends INameable> Collection<T> filterWildcard(Collection<T> inputValues, String pattern) {
        return inputValues.stream()
                .filter(t -> t.getName().matches(pattern.replace("*", "(.*)")))
                .collect(Collectors.toList());
    }

    public static boolean anyMatch(Collection<? extends INameable> values, String pattern) {
        return values.stream()
                .anyMatch(t -> t.getName().matches(pattern.replace("*", "(.*)")));
    }

}
