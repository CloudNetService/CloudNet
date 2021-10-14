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

package de.dytanic.cloudnet.launcher.version;

import java.nio.file.Path;

public abstract class DefaultVersionInfo implements VersionInfo {

  protected String repositoryVersion;
  protected String appVersion;

  protected String gitHubRepository;

  protected long releaseTimestamp;

  protected Path targetDirectory;

  @Override
  public String getRepositoryVersion() {
    return this.repositoryVersion;
  }

  @Override
  public String getCurrentVersion() {
    return this.appVersion;
  }

  @Override
  public String getGitHubRepository() {
    return this.gitHubRepository;
  }

  @Override
  public long getReleaseTimestamp() {
    return this.releaseTimestamp;
  }

  @Override
  public Path getTargetDirectory() {
    return this.targetDirectory;
  }
}
