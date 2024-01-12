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

package eu.cloudnetservice.ext.platforminject.processor.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class ResourceUtil {

  private static Path resourcesDirectory;

  private ResourceUtil() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable Path resolveResource(@NonNull Filer filer, @NonNull String expectedName) {
    // get or resolve the resources directory
    var resourcesDirectory = resolveResourcesDirectory(filer);
    if (resourcesDirectory == null) {
      return null;
    }

    // resolve the file in the resources directory
    return resolveResource(resourcesDirectory, expectedName);
  }

  private static @Nullable Path resolveResource(@NonNull Path resourcesDirectory, @NonNull String expectedName) {
    try (var stream = Files.newDirectoryStream(resourcesDirectory)) {
      for (var candidate : stream) {
        // if the file is a directory try to resolve in the given directory
        if (Files.isDirectory(candidate)) {
          var foundPath = resolveResource(candidate, expectedName);
          if (foundPath != null) {
            return foundPath;
          }
          // don't check the filename
          continue;
        }

        // check if the filename matches the expected name
        var fileName = Objects.toString(candidate.getFileName(), null);
        if (fileName != null && fileName.equals(expectedName)) {
          return candidate;
        }
      }
    } catch (IOException ignored) {
    }
    // unable to resolve
    return null;
  }

  private static @Nullable Path resolveResourcesDirectory(@NonNull Filer filer) {
    // only resolve the directory once
    if (resourcesDirectory != null) {
      return resourcesDirectory;
    }

    var attempts = 0;
    while (attempts++ <= 5) {
      // dummy file which will be deleted when the method execution finishes
      FileObject dummyFile = null;

      try {
        // create a dummy resource to get the target directory of source files
        var dummyFileName = UUID.randomUUID().toString().replace("-", "") + ".dummy";
        dummyFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", dummyFileName);

        // walk up the tree until we find the first resources directory
        var candidate = Path.of(dummyFile.toUri());
        do {
          // check if the current directory has a resources subdirectory
          var resourcesDirectory = candidate.resolve("resources");
          if (Files.exists(resourcesDirectory) && Files.isDirectory(resourcesDirectory)) {
            // cache & return
            ResourceUtil.resourcesDirectory = candidate;
            return candidate;
          }
        } while ((candidate = candidate.getParent()) != null);

        // unable to resolve the directory
        return null;
      } catch (FilerException ignored) {
        // some filer guarantee was violated by the file creation, just try again
      } catch (IOException exception) {
        // unable to create the dummy file?
        throw new IllegalStateException("Creation of dummy file to resolve resources directory failed", exception);
      } finally {
        // remove the dummy file, if created
        if (dummyFile != null) {
          dummyFile.delete();
        }
      }
    }

    // unable to resolve the directory
    return null;
  }
}
