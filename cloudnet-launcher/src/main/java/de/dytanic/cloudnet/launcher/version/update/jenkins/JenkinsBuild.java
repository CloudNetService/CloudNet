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

package de.dytanic.cloudnet.launcher.version.update.jenkins;

import java.util.List;

public class JenkinsBuild {

  private final List<BuildArtifact> artifacts;

  private final long timestamp;

  public JenkinsBuild(List<BuildArtifact> artifacts, long timestamp) {
    this.artifacts = artifacts;
    this.timestamp = timestamp;
  }

  public List<BuildArtifact> getArtifacts() {
    return this.artifacts;
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public static class BuildArtifact {

    private final String fileName;

    private final String relativePath;

    public BuildArtifact(String fileName, String relativePath) {
      this.fileName = fileName;
      this.relativePath = relativePath;
    }

    public String getFileName() {
      return this.fileName;
    }

    public String getRelativePath() {
      return this.relativePath;
    }

  }

}
