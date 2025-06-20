/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.resource.ResourceResolver;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import lombok.NonNull;

/**
 * Defines the necessary classes to replace {@code sun.misc.Unsafe} in the bootstrap class loader. This must be done as
 * the unsafe class is loaded by that class loader, so the replacement classes must be in that class loader as well. To
 * prevent other classes of the wrapper from getting accidentally defined in the bootstrap class loader, the necessary
 * classes are moved into a separate jar which is appended to the bootstrap class loader.
 *
 * @since 4.0
 */
final class UnsafeReplacementDefiner {

  // classes that should be appended to the bootstrap class path
  private static final Set<String> BOOTSTRAP_CLASS_PREFIXES = Set.of(
    "ArrayOps",
    "FieldOps",
    "LazyMemoizingSupplier",
    "MemoryControlOps",
    "MemoryOps",
    "OpConstants",
    "UnsafeLogUtil",
    "UnsafeReplacement.class", // full name as some other classes have this prefix
    "UnsafeReplacementDelegate",
    "UnsafeUsageTraceLogger",
    "ValueTypeKind"
  );

  // temp jar file that contains only the classes for the bootstrap class path
  private static final Path TEMP_JAR_FILE = Path.of(".wrapper", "unsafe_replacement.jar");

  // instrumentation instance that can be used to define the classes
  private static Instrumentation instrumentation;
  // if the classes were appended to the boostrap class path
  private static boolean classesAppended = false;

  private UnsafeReplacementDefiner() {
    throw new UnsupportedOperationException();
  }

  /**
   * Inits the instrumentation to use for defining the classes in the bootstrap class loader.
   *
   * @param instrumentation the instrumentation to use.
   * @throws NullPointerException  if the given classloader is null.
   * @throws IllegalStateException if the instrumentation is already initialized.
   */
  static void init(@NonNull Instrumentation instrumentation) {
    if (UnsafeReplacementDefiner.instrumentation == null) {
      UnsafeReplacementDefiner.instrumentation = instrumentation;
      return;
    }

    throw new IllegalStateException();
  }

  /**
   * Appends all necessary classes for unsafe replacement to the bootstrap class loader by copying them into a newly
   * created jar file.
   *
   * @throws UncheckedIOException if an I/O exception occurs.
   */
  static void appendClassesToBootstrapClassLoader() {
    try {
      appendClassesToBootstrapClassLoaderInner();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  /**
   * Appends all necessary classes for unsafe replacement to the bootstrap class loader by copying them into a newly
   * created jar file. Internal method that does throw a checked i/o exception.
   *
   * @throws IOException if an I/O exception occurs.
   */
  private static void appendClassesToBootstrapClassLoaderInner() throws IOException {
    if (UnsafeReplacementDefiner.classesAppended || UnsafeReplacementDefiner.instrumentation == null) {
      return;
    }

    UnsafeReplacementDefiner.classesAppended = true;

    // copy all classes that should be in the bootstrap class loader into a separate jar
    var packageDirName = UnsafeReplacementDefiner.class.getPackageName().replace('.', '/');
    try (var out = new JarOutputStream(Files.newOutputStream(TEMP_JAR_FILE))) {
      var resourcePath = Path.of(ResourceResolver.resolveCodeSourceOfClass(UnsafeReplacementDefiner.class));
      FileUtil.openZipFile(resourcePath, fs -> {
        var packageDir = fs.getPath(packageDirName);
        FileUtil.walkFileTree(packageDir, (_, file) -> {
          var fileName = file.getFileName().toString();
          var shouldBeInBootstrapLoader = BOOTSTRAP_CLASS_PREFIXES.stream().anyMatch(fileName::startsWith);
          if (shouldBeInBootstrapLoader) {
            var classDirName = packageDirName + '/' + fileName;
            try (var classStream = Files.newInputStream(file)) {
              var jarEntry = new JarEntry(classDirName);
              out.putNextEntry(jarEntry);
              classStream.transferTo(out);
              out.closeEntry();
            } catch (IOException exception) {
              throw new UncheckedIOException(exception);
            }
          }
        }, false, "*.class");
      });
    }

    // append the newly created jar file to the bootstrap class loader search
    var jarFileFile = TEMP_JAR_FILE.toFile();
    var jarFile = new JarFile(jarFileFile);
    UnsafeReplacementDefiner.instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
  }
}
