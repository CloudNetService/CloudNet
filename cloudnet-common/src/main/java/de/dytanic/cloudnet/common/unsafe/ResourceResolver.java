package de.dytanic.cloudnet.common.unsafe;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.annotation.UnsafeClass;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Allows you to provide the URI of a class whose classpath item
 */
@UnsafeClass
public final class ResourceResolver {

    private ResourceResolver() {
        throw new UnsupportedOperationException();
    }

    /**
     * Allows you to provide the URI of a class whose classpath item
     *
     * @param clazz the class, which should resolve the classpath item URI from
     * @return the uri of the classpath element or null if an exception was caught
     * @see Class
     * @see java.security.ProtectionDomain
     * @see java.security.CodeSource
     */
    public static URI resolveURIFromResourceByClass(Class<?> clazz) {
        Validate.checkNotNull(clazz);

        try {
            return clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException exception) {
            exception.printStackTrace();
        }

        return null;
    }

}