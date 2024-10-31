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

package eu.cloudnetservice.node.impl.version.information;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.node.impl.version.ServiceVersion;
import eu.cloudnetservice.node.impl.version.ServiceVersionType;
import eu.cloudnetservice.utils.base.StringUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class FileSystemVersionInstaller extends VersionInstaller {

  private final Path workingDirectory;

  public FileSystemVersionInstaller(
    @NonNull ServiceVersion serviceVersion,
    @NonNull ServiceVersionType serviceVersionType,
    boolean cacheFiles,
    @Nullable String installerExecutable,
    @NonNull Path workingDirectory
  ) {
    super(serviceVersion, serviceVersionType, cacheFiles, installerExecutable);
    this.workingDirectory = workingDirectory;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  public void deployFile(@NonNull InputStream source, @NonNull String target) {
    var targetPath = this.workingDirectory.resolve(target);
    FileUtil.ensureChild(this.workingDirectory, targetPath);

    FileUtil.copy(source, targetPath);
  }

  @Override
  public void removeServiceVersions(@NonNull Collection<ServiceVersionType> knownTypes) throws IOException {
    try (var stream = Files.newDirectoryStream(this.workingDirectory, "*.jar")) {
      for (var path : stream) {
        var fileName = path.getFileName().toString();
        for (var environment : knownTypes) {
          if (StringUtil.toLower(fileName).contains(environment.name())) {
            Files.deleteIfExists(path);
          }
        }
      }
    }
  }

  public static final class Builder extends VersionInstaller.Builder<FileSystemVersionInstaller, Builder> {

    private Path workingDirectory;

    public @NonNull Builder workingDirectory(@NonNull Path workingDirectory) {
      this.workingDirectory = workingDirectory;
      return this;
    }

    @Override
    protected @NonNull FileSystemVersionInstaller doBuild() {
      Preconditions.checkNotNull(this.workingDirectory, "working directory must be given");
      return new FileSystemVersionInstaller(
        this.serviceVersion,
        this.serviceVersionType,
        this.cacheFiles,
        this.installerExecutable,
        this.workingDirectory);
    }
  }
}
