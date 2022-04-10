/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.node.version.information;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.node.version.ServiceVersion;
import eu.cloudnetservice.node.version.ServiceVersionType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class VersionInstaller {

  private final ServiceVersion serviceVersion;
  private final ServiceVersionType serviceVersionType;
  private final boolean cacheFiles;
  private final String installerExecutable;

  public VersionInstaller(
    @NonNull ServiceVersion serviceVersion,
    @NonNull ServiceVersionType serviceVersionType,
    boolean cacheFiles,
    @Nullable String installerExecutable
  ) {
    this.serviceVersion = serviceVersion;
    this.serviceVersionType = serviceVersionType;
    this.cacheFiles = cacheFiles;
    this.installerExecutable = installerExecutable;
  }

  public @NonNull Optional<String> installerExecutable() {
    return Optional.ofNullable(this.installerExecutable);
  }

  public @NonNull ServiceVersion serviceVersion() {
    return serviceVersion;
  }

  public @NonNull ServiceVersionType serviceVersionType() {
    return serviceVersionType;
  }

  public boolean cacheFiles() {
    return cacheFiles;
  }

  public abstract void deployFile(@NonNull InputStream source, @NonNull String target) throws IOException;

  public abstract void removeServiceVersions(@NonNull Collection<ServiceVersionType> knownTypes) throws IOException;

  public abstract static class Builder<R extends VersionInstaller, B extends Builder<R, B>> {

    protected ServiceVersion serviceVersion;
    protected ServiceVersionType serviceVersionType;

    protected boolean cacheFiles;
    protected String installerExecutable;

    public @NonNull B serviceVersion(@NonNull ServiceVersion serviceVersion) {
      this.serviceVersion = serviceVersion;
      this.cacheFiles = serviceVersion.cacheFiles();
      return this.self();
    }

    public @NonNull B serviceVersionType(@NonNull ServiceVersionType serviceVersionType) {
      this.serviceVersionType = serviceVersionType;
      return this.self();
    }

    public @NonNull B cacheFiles(boolean cacheFiles) {
      this.cacheFiles = cacheFiles;
      return this.self();
    }

    public @NonNull B executable(@Nullable String installerExecutable) {
      this.installerExecutable = installerExecutable;
      return this.self();
    }

    @SuppressWarnings("unchecked")
    protected @NonNull B self() {
      return (B) this;
    }

    public @NonNull R build() {
      Preconditions.checkNotNull(this.serviceVersion, "No service version specified");
      Preconditions.checkNotNull(this.serviceVersionType, "No version type specified");

      return this.doBuild();
    }

    protected abstract @NonNull R doBuild();
  }
}
