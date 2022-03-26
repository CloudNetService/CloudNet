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

package eu.cloudnetservice.cloudnet.driver;

import java.util.regex.Pattern;
import lombok.NonNull;

/**
 * Represents the current version specification which the associated CloudNet component is running on.
 *
 * @param major        the current major version of CloudNet.
 * @param minor        the current minor version of CloudNet.
 * @param patch        the current patch number of the current CloudNet minor version.
 * @param revision     the git revision CloudNet is running on, empty if unknown.
 * @param versionType  the current version type of CloudNet (for example snapshot).
 * @param versionTitle the current release title of CloudNet.
 * @since 4.0
 */
public record CloudNetVersion(
  int major,
  int minor,
  int patch,
  @NonNull String revision,
  @NonNull String versionType,
  @NonNull String versionTitle
) {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+).(\\d+).(\\d+)(.*)");

  /**
   * Reads and tries to parse the CloudNet version from the information provided by the specified package. This method
   * throws an exception if the given package does not contain valid version information. The version of the package
   * must at least match the semver specification, MAJOR.MINOR.PATCH. If any additional information is supplied this
   * method will try to parse the remaining information from it (revision, version type). The version title is
   * determined by the package implementation title.
   *
   * @param source the package to parse the CloudNet version from.
   * @return the parsed CloudNet version instance from the given package.
   * @throws NullPointerException     if the given package is null.
   * @throws IllegalArgumentException if the given package doesn't contain parseable data.
   */
  public static @NonNull CloudNetVersion fromPackage(@NonNull Package source) {
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
    throw new IllegalArgumentException("Unable to determine version from " + source.getImplementationVersion());
  }

  /**
   * Converts this version back to a human-readable string. The returned version string always follows the schema:
   * <pre>
   * CloudNet &lt;VersionTitle&gt; &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;-&lt;VersionType&gt; [revision]
   * </pre>
   * The revision is only appended if present (not empty).
   *
   * @return the stringified, human-readable representation of this version.
   */
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
