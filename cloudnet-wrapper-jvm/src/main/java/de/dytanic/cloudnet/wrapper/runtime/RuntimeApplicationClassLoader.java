package de.dytanic.cloudnet.wrapper.runtime;

import lombok.Getter;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class loader is a bridge which should replace
 * the AppClassLoader and access the AppClassLoader if classes are not found.
 * This allows the actual service to access the benefits of the SystemClassLoader.
 */
public final class RuntimeApplicationClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    @Getter
    private final ClassLoader cloudNetWrapperClassLoader;

    public RuntimeApplicationClassLoader(ClassLoader cloudNetWrapperClassLoader, URL url, ClassLoader parent) {
        super(new URL[]{url}, parent);

        this.cloudNetWrapperClassLoader = cloudNetWrapperClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            Class<?> loaded = this.findLoadedClass(name);
            if (loaded != null)
                return loaded;
            else
                return super.loadClass(name, resolve);

        } catch (Throwable ex) {
            return cloudNetWrapperClassLoader.loadClass(name);
        }
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);

        if (url == null)
            url = cloudNetWrapperClassLoader.getResource(name);

        return url;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) || cloudNetWrapperClassLoader.equals(obj);
    }
}