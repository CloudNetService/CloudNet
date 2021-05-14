package de.dytanic.cloudnet.ext.rest;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;

import java.util.ArrayList;

public final class RestUtils {

    private RestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void replaceNulls(ServiceConfigurationBase configuration) {
        if (configuration.getTemplates() == null) {
            configuration.setTemplates(new ArrayList<>());
        }
        if (configuration.getIncludes() == null) {
            configuration.setIncludes(new ArrayList<>());
        }
        if (configuration.getDeployments() == null) {
            configuration.setDeployments(new ArrayList<>());
        }
    }

    public static <T> T getFirst(Iterable<T> iterable) {
        return getFirst(iterable, null);
    }

    public static <T> T getFirst(Iterable<T> iterable, T def) {
        if (iterable == null) {
            return def;
        } else {
            return Iterables.getFirst(iterable, def);
        }
    }
}
