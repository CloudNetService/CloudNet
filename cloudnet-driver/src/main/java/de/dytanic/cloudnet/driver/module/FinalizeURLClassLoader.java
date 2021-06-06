package de.dytanic.cloudnet.driver.module;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FinalizeURLClassLoader extends URLClassLoader {

  private static final Collection<FinalizeURLClassLoader> CLASS_LOADERS = new CopyOnWriteArrayList<>();

  static {
    ClassLoader.registerAsParallelCapable();
  }

  public FinalizeURLClassLoader(URL[] urls) {
    super(urls, FinalizeURLClassLoader.class.getClassLoader());

    CLASS_LOADERS.add(this);
  }

  public FinalizeURLClassLoader(URL url) {
    this(new URL[]{url});
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException {
    try {
      return this.loadClass0(name, resolve);
    } catch (ClassNotFoundException ignored) {
    }

    for (FinalizeURLClassLoader classLoader : CLASS_LOADERS) {
      if (classLoader != this) {
        try {
          return classLoader.loadClass0(name, resolve);
        } catch (ClassNotFoundException ignored) {
        }
      }
    }

    throw new ClassNotFoundException(name);
  }

  private Class<?> loadClass0(String name, boolean resolve)
    throws ClassNotFoundException {
    return super.loadClass(name, resolve);
  }

  @Override
  public void close() throws IOException {
    super.close();
    CLASS_LOADERS.remove(this);
  }
}
