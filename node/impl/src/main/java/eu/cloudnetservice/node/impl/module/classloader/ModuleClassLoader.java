/*
 * Copyright 2019-present CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.node.impl.module.classloader;

import eu.cloudnetservice.node.impl.module.classloader.transformer.ModuleClassTransformer;
import eu.cloudnetservice.node.module.metadata.ModuleMetadata;
import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import lombok.NonNull;

/**
 * Class loader that is able to resolve classes in the module jar. External classes, such as classes from libraries or
 * classes from other modules are resolved by other loaders higher up in the hierarchy. Note that this loader prefers
 * classes from the module file over classes from one of the parent class loaders.
 *
 * @since 4.0
 */
public final class ModuleClassLoader extends SecureClassLoader implements Closeable {

  static {
    ClassLoader.registerAsParallelCapable();
  }

  private final URL moduleJarUrl;
  private final JarFile moduleJarFile;
  private final Manifest moduleJarManifest;
  private final ModuleMetadata moduleMetadata;
  private final ModuleClassTransformer classTransformer;

  /**
   * Constructs a new class loader for a specific module.
   *
   * @param moduleJarUrl     the url to the jar file of the module associated with this class loader.
   * @param moduleJarFile    the open jar file of the module associated with this class loader.
   * @param moduleMetadata   the parsed metadata of the module associated with this class loader.
   * @param classTransformer transformer to apply to classes loaded by this class loader.
   * @param parent           the parent of this class loader.
   * @throws NullPointerException if one the given parameters is null.
   * @throws IOException          if an i/o exception occurs while reading the module manifest.
   */
  public ModuleClassLoader(
    @NonNull URL moduleJarUrl,
    @NonNull JarFile moduleJarFile,
    @NonNull ModuleMetadata moduleMetadata,
    @NonNull ModuleClassTransformer classTransformer,
    @NonNull ClassLoader parent
  ) throws IOException {
    super("module-cl:" + moduleMetadata.id(), parent);

    this.moduleJarUrl = moduleJarUrl;
    this.moduleJarFile = moduleJarFile;
    this.moduleJarManifest = moduleJarFile.getManifest();
    this.moduleMetadata = moduleMetadata;
    this.classTransformer = classTransformer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Class<?> loadClass(@NonNull String name, boolean resolve) throws ClassNotFoundException {
    var classLoadingLock = super.getClassLoadingLock(name);
    synchronized (classLoadingLock) {
      var result = super.findLoadedClass(name);
      if (result == null) {
        // try to find the class in the module file
        try {
          result = this.findClass(name);
        } catch (ClassNotFoundException _) {
        }
      }

      if (result == null) {
        // try to load the class from the parent class loader
        var parent = super.getParent();
        result = parent.loadClass(name);
      }

      if (resolve) {
        // resolve (link) the class if requested
        this.resolveClass(result);
      }

      return result;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected @NonNull Class<?> findClass(@NonNull String name) throws ClassNotFoundException {
    // try to resolve the jar entry of the class from the module jar
    JarEntry classEntry;
    try {
      var classFilePath = name.replace('.', '/').concat(".class");
      classEntry = this.moduleJarFile.getJarEntry(classFilePath);
      if (classEntry == null) {
        throw new ClassNotFoundException(name);
      }
    } catch (IllegalStateException exception) {
      var msg = String.format("Failed to resolve class '%s' using loader of module %s", name, this.moduleMetadata.id());
      var internalCause = new IllegalStateException(msg, exception);
      throw new ClassNotFoundException(name, internalCause);
    }

    // resolve the raw class file bytes from the module jar
    byte[] rawClassFile;
    try (var classFileInputStream = this.moduleJarFile.getInputStream(classEntry)) {
      rawClassFile = classFileInputStream.readAllBytes();
    } catch (IOException exception) {
      var msg = String.format("Failed to read class '%s' in loader of module %s", name, this.moduleMetadata.id());
      var internalCause = new IOException(msg, exception);
      throw new ClassNotFoundException(name, internalCause);
    }

    // apply transformers to the raw class file
    var transformedClassFile = this.classTransformer.transformClass(rawClassFile, this, this.moduleMetadata);

    // define the package in which the class is located
    var lastPackageDelimPos = name.lastIndexOf('.');
    if (lastPackageDelimPos > 0) {
      try {
        var pkgName = name.substring(0, lastPackageDelimPos);
        var definedPackage = super.getDefinedPackage(pkgName);
        if (definedPackage == null) {
          if (this.moduleJarManifest != null) {
            this.definePackageWithAttributes(pkgName);
          } else {
            super.definePackage(pkgName, null, null, null, null, null, null, null);
          }
        }
      } catch (IllegalArgumentException _) {
        var definedPackage = super.getDefinedPackage(name);
        if (definedPackage == null) {
          // should be defined at this point...?
          throw new IllegalStateException("Unable to define package for class " + name);
        }
      }
    }

    // define the final transformed class
    var signers = classEntry.getCodeSigners();
    var codeSource = new CodeSource(this.moduleJarUrl, signers);
    return super.defineClass(name, transformedClassFile, 0, transformedClassFile.length, codeSource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    this.moduleJarFile.close();
  }

  /**
   * Defines a new package with the given name in this loader. The attributes contained in the manifest of the module
   * will be used to obtain package version and sealing information. For sealed packages, the module url will be used as
   * the code source URL from which the package was loaded. This method must only be used in case the module jar
   * manifest is non-null.
   *
   * @param packageName the name of the package to define.
   * @throws NullPointerException if the given package name is null, or the module jar did not contain a manifest.
   */
  private void definePackageWithAttributes(@NonNull String packageName) {
    var attributeHolder = new PackageAttributeHolder();

    // first fill in the package-specific attributes
    var packageEntryName = packageName.replace('.', '/').concat("/");
    var packageAttributes = this.moduleJarManifest.getAttributes(packageEntryName);
    if (packageAttributes != null) {
      attributeHolder.fillFromAttributes(packageAttributes);
    }

    // then fill in the remaining attributes from the main manifest
    var mainAttributes = this.moduleJarManifest.getMainAttributes();
    attributeHolder.fillFromAttributes(mainAttributes);

    // define the package, optionally providing the seal code source url, if the jar was marked as sealed
    var sealed = Boolean.parseBoolean(attributeHolder.sealed);
    var sealBase = sealed ? this.moduleJarUrl : null;
    super.definePackage(
      packageName,
      attributeHolder.specTitle,
      attributeHolder.specVersion,
      attributeHolder.specVendor,
      attributeHolder.implTitle,
      attributeHolder.implVersion,
      attributeHolder.implVendor,
      sealBase);
  }

  /**
   * Get the metadata of the module this class loader was constructed for.
   *
   * @return the metadata of the module this class loader was constructed for.
   */
  public @NonNull ModuleMetadata moduleMetadata() {
    return this.moduleMetadata;
  }

  /**
   * Holder for attributes that can be defined in the manifest of a jar.
   *
   * @since 4.0
   */
  private static final class PackageAttributeHolder {

    private String specTitle;
    private String specVersion;
    private String specVendor;
    private String implTitle;
    private String implVersion;
    private String implVendor;
    private String sealed;

    /**
     * Fills the relevant attributes from the given attributes. Only attributes that were not set previously will be
     * changed as a result of this method invocation.
     *
     * @param attributes the attributes to resolve from.
     * @throws NullPointerException if the given attributes are null.
     */
    public void fillFromAttributes(@NonNull Attributes attributes) {
      if (this.specTitle == null) {
        this.specTitle = attributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
      }
      if (this.specVersion == null) {
        this.specVersion = attributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
      }
      if (this.specVendor == null) {
        this.specVendor = attributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
      }
      if (this.implTitle == null) {
        this.implTitle = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
      }
      if (this.implVersion == null) {
        this.implVersion = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
      }
      if (this.implVendor == null) {
        this.implVendor = attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
      }
      if (this.sealed == null) {
        this.sealed = attributes.getValue(Attributes.Name.SEALED);
      }
    }
  }
}
