package de.dytanic.cloudnet.ext.bridge.sponge.util;

import lombok.Getter;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public final class SpongeCloudNetAdapterClassLoader extends URLClassLoader {

    @Getter
    private final ClassLoader cloudNetClassLoader, spongeAppClassLoader;

    public SpongeCloudNetAdapterClassLoader(ClassLoader cloudNetClassLoader, ClassLoader spongeAppClassLoader, ClassLoader parent) {
        super(new URL[0], parent);

        this.cloudNetClassLoader = cloudNetClassLoader;
        this.spongeAppClassLoader = spongeAppClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> target = findClass0(name);

        if (target != null) return target;

        try {
            Class<?> loaded = this.findLoadedClass(name);
            if (loaded != null)
                return loaded;
            else
                return super.loadClass(name, resolve);

        } catch (Throwable ex) {
            if (name.startsWith("io.netty") || name.startsWith("com.google.gson"))
                throw new ClassNotFoundException(name);
            return cloudNetClassLoader.loadClass(name);
        }
    }

    private Class<?> findClass0(String name) {
        try {

            Method method = URLClassLoader.class.getMethod("findClass", String.class);
            method.setAccessible(true);

            return (Class<?>) method.invoke(spongeAppClassLoader, name);

        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);

        if (url == null)
            url = cloudNetClassLoader.getResource(name);

        return url;
    }

}