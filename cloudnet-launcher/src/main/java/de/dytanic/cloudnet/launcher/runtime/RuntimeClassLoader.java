package de.dytanic.cloudnet.launcher.runtime;

import java.net.URL;
import java.net.URLClassLoader;

public final class RuntimeClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public RuntimeClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}