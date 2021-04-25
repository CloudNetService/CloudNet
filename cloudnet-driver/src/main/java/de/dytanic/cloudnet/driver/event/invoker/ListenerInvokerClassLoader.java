package de.dytanic.cloudnet.driver.event.invoker;

import java.security.SecureClassLoader;

/**
 * {@link SecureClassLoader} giving the possibility to define new classes.
 */
public class ListenerInvokerClassLoader extends SecureClassLoader {

    public ListenerInvokerClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String className, byte[] bytes) {
        return super.defineClass(className, bytes, 0, bytes.length);
    }
}
