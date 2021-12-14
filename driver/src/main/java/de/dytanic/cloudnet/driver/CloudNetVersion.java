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

package de.dytanic.cloudnet.driver;

import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public class CloudNetVersion {

  protected static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+).(\\d+).(\\d+)(.*)");

  private final int major;
  private final int minor;
  private final int patch;
  private final String revision;
  private final String versionType;
  private final String versionTitle;

  public CloudNetVersion(int major, int minor, int patch, String revision, String versionType, String versionTitle) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.revision = revision;
    this.versionType = versionType;
    this.versionTitle = versionTitle;
  }

  public static @NotNull CloudNetVersion fromClassInformation(@NotNull Package source) {
    // read the version title
    var title = source.getImplementationTitle();
    // read the version
    var matcher = VERSION_PATTERN.matcher(source.getImplementationVersion());
    if (matcher.matches()) {
      // extract the semver information
      var major = Integer.parseInt(matcher.group(1));
      var minor = Integer.parseInt(matcher.group(2));
      var patch = Integer.parseInt(matcher.group(3));
      // check if we have additional information
      if (matcher.groupCount() > 3) {
        // fetch additional information from the version
        var extraInformation = matcher.group(4);
        // we always assume that we have at least on '-' in the rest of the capturing
        // which equals for a full version string: 3.4.6-alpha-dev-556hfz4
        // this should result in versionType: 'alpha-dev', revision: '556hfz4'
        var lastSplit = extraInformation.lastIndexOf('-');
        if (lastSplit != -1 && lastSplit < extraInformation.length()) {
          // extract extra information
          var revision = extraInformation.substring(lastSplit + 1);
          // extract the version type
          String versionType;
          if (lastSplit == 0) {
            // an empty version type is a release (all other types are suffixed, for example '-SNAPSHOT')
            versionType = "RELEASE";
          } else {
            // 1 to exclude the initial '-'
            versionType = extraInformation.substring(1, lastSplit);
          }
          // an empty version
          // construct the version information based on the extra information
          return new CloudNetVersion(major, minor, patch, revision, versionType, title);
        }
      }
      // construct without the revision and version type
      return new CloudNetVersion(major, minor, patch, "", "custom", title);
    }
    // unable to determine version information
    throw new RuntimeException("Unable to determine version from " + source.getImplementationVersion());
  }

  public int getMajor() {
    return this.major;
  }

  public int getMinor() {
    return this.minor;
  }

  public int getPatch() {
    return this.patch;
  }

  public @NotNull String getRevision() {
    return this.revision;
  }

  public @NotNull String getVersionType() {
    return this.versionType;
  }

  public @NotNull String getVersionTitle() {
    return this.versionTitle;
  }

  @Override
  public String toString() {
    // CloudNet Blizzard 3.5.0-SNAPSHOT
    var toString =
      "CloudNet " + this.versionTitle + ' ' + this.major + '.' + this.minor + '.' + this.patch + '-' + this.versionType;
    if (!this.revision.isEmpty()) {
      // CloudNet Blizzard 3.5.0-SNAPSHOT 5dd7f63
      toString += ' ' + this.revision;
    }
    return toString;
  }
}
