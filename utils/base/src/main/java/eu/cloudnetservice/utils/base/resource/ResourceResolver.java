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

package eu.cloudnetservice.utils.base.resource;

import eu.cloudnetservice.utils.base.io.FileUtil;
import io.vavr.CheckedConsumer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * A utility class to resolve the location of class resource on the class path or file system.
 *
 * @since 4.0
 */
public final class ResourceResolver {

  private ResourceResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves the parent location of the given class on the file system or on the class path. This means that the method
   * either returns the jar file that contains the given class or the directory in which the given class is located.
   *
   * @param caller the class to get the parent location of.
   * @return a URI pointing to the parent location of the given class.
   * @throws NullPointerException     if the given class is null.
   * @throws IllegalStateException    if there is an error resolving the parent class uri.
   * @throws IllegalArgumentException if a parent uri candidate was found but couldn't be converted into a URI object.
   */
  public static @NonNull URI resolveCodeSourceOfClass(@NonNull Class<?> caller) {
    try {
      // try via class code source first
      var classCodeSource = caller.getProtectionDomain().getCodeSource();
      if (classCodeSource != null) {
        var codeLocation = classCodeSource.getLocation();
        if (codeLocation != null) {
          return codeLocation.toURI();
        }
      }
    } catch (URISyntaxException ignored) {
    }

    // try via class url instead
    var callingClassResource = caller.getResource(caller.getSimpleName() + ".class");
    if (callingClassResource == null) {
      throw resolveError(caller, "missing possibility to resolve class url");
    }

    // sanity check: validate that we actually got the correct url
    var fullResourceUrl = callingClassResource.toExternalForm();
    var expectedUrlSuffix = caller.getCanonicalName().replace('.', '/') + ".class";
    if (!fullResourceUrl.endsWith(expectedUrlSuffix)) {
      throw resolveError(
        caller,
        "unexpected suffix on url " + fullResourceUrl + " (expected: " + expectedUrlSuffix + ")");
    }

    // strip the class information from the url
    var strippedUrl = fullResourceUrl.substring(0, fullResourceUrl.length() - expectedUrlSuffix.length());

    // remove the jar protocol from the uri (if necessary)
    if (strippedUrl.startsWith("jar:")) {
      strippedUrl = strippedUrl.substring(4);
    }

    // remove the "!/" suffix (if necessary)
    if (strippedUrl.endsWith("!/")) {
      strippedUrl = strippedUrl.substring(0, strippedUrl.length() - 2);
    }

    // construct the uri from the cleaned url
    return URI.create(strippedUrl);
  }

  /**
   * Finds the code source of the caller and passes it to the given consumer. The passed path can either be the root of
   * a zip file system (if the code source is located in a jar) or the absolute path to a directory on the system (if
   * the source is a file on the default file system).
   *
   * @param caller   the class to get the parent location of.
   * @param consumer the consumer to which the code source root path should be passed.
   * @throws NullPointerException  if the given caller or consumer is null.
   * @throws IllegalStateException if an exception occurs while executing the given consumer.
   */
  public static void openCodeSourceRoot(@NonNull Class<?> caller, @NonNull CheckedConsumer<Path> consumer) {
    try {
      var codeSource = resolveCodeSourceOfClass(caller);
      var codeSourcePath = Path.of(codeSource);
      if (codeSource.getScheme().equals("jar") || !Files.isDirectory(codeSourcePath)) {
        // the code source is a jar file, we need to open a zip file
        // system of that jar and pass the root path of that to the caller
        FileUtil.openZipFile(codeSourcePath, fs -> {
          var rootPath = fs.getPath("");
          consumer.accept(rootPath);
        });
      } else {
        // the code source is not a jar, presumably it's a file on the default file system
        // maybe there are more cases which aren't handled by this method
        consumer.accept(codeSourcePath);
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException("Issue while handling code source of " + caller, throwable);
    }
  }

  /**
   * Constructs a new, unified illegal state exception that can be thrown to indicate that there was some error
   * resolving the URI of a class. The returned exceptions contains the class name and the given error as its detail
   * message.
   *
   * @param caller the class to get the parent location of.
   * @param reason the reason why the uri resolution process failed.
   * @return a new illegal state exception using a unified detail message based on the given caller class and reason.
   * @throws NullPointerException if the given caller class or reason is null.
   */
  private static @NonNull IllegalStateException resolveError(@NonNull Class<?> caller, @NonNull String reason) {
    return new IllegalStateException("Unable to resolve uri of class " + caller + " due to " + reason);
  }
}
