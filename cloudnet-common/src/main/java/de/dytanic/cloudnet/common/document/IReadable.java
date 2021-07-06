/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common.document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface is interesting to read data, of the implement object
 */
public interface IReadable {

  @NotNull
  IReadable read(@NotNull Reader reader);

  @NotNull
  IReadable read(byte[] bytes);

  @NotNull
  IReadable append(@NotNull Reader reader);

  @NotNull
  IReadable read(@NotNull InputStream inputStream);

  @NotNull
  default IReadable read(@Nullable Path path) {
    if (path != null && Files.exists(path)) {
      try (InputStream inputStream = Files.newInputStream(path)) {
        return this.read(inputStream);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    return this;
  }

  @NotNull
  default IReadable read(@Nullable String path) {
    if (path != null) {
      return this.read(Paths.get(path));
    }
    return this;
  }

  @NotNull
  default IReadable read(@Nullable String... paths) {
    if (paths != null) {
      for (String path : paths) {
        this.read(path);
      }
    }
    return this;
  }

  @NotNull
  @Deprecated
  default IReadable read(@Nullable File file) {
    if (file != null) {
      return this.read(file.toPath());
    }
    return this;
  }

  @NotNull
  @Deprecated
  default IReadable read(@Nullable File... files) {
    if (files != null) {
      for (File file : files) {
        this.read(file);
      }
    }
    return this;
  }

  @NotNull
  default IReadable read(@Nullable Path... paths) {
    if (paths != null) {
      for (Path path : paths) {
        this.read(path);
      }
    }
    return this;
  }

  @NotNull
  default IReadable append(@NotNull InputStream inputStream) {
    try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      return this.append(reader);
    } catch (IOException exception) {
      exception.printStackTrace();
      return this;
    }
  }

  @NotNull
  default IReadable append(@Nullable Path path) {
    this.read(path);
    return this;
  }

  @NotNull
  default IReadable append(@Nullable String path) {
    if (path != null) {
      return this.append(Paths.get(path));
    }
    return this;
  }

  @NotNull
  default IReadable append(@Nullable String... paths) {
    if (paths != null) {
      for (String path : paths) {
        this.append(path);
      }
    }
    return this;
  }

  @NotNull
  @Deprecated
  default IReadable append(@Nullable File file) {
    if (file != null) {
      return this.append(file.toPath());
    }
    return this;
  }

  @NotNull
  @Deprecated
  default IReadable append(@Nullable File... files) {
    if (files != null) {
      for (File file : files) {
        this.append(file);
      }
    }
    return this;
  }

  @NotNull
  default IReadable append(@Nullable Path... paths) {
    if (paths != null) {
      for (Path path : paths) {
        this.append(path);
      }
    }
    return this;
  }
}
