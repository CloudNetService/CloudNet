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

package eu.cloudnetservice.modules.s3.config;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class S3TemplateStorageConfig {

  private static final Logger LOGGER = LogManager.logger(S3TemplateStorageConfig.class);

  private final String name;
  private final String bucket;

  private final String region;
  private final boolean dualstackEndpointEnabled;

  private final String accessKey;
  private final String secretKey;

  private final String endpointOverride;

  public S3TemplateStorageConfig() {
    this("s3", "cloudnet", "eu-west-1", false, "secret", "more_secret", null);
  }

  public S3TemplateStorageConfig(
    @NotNull String name,
    @NotNull String bucket,
    @NotNull String region,
    boolean dualstackEndpointEnabled,
    @NotNull String accessKey,
    @NotNull String secretKey,
    @Nullable String endpointOverride
  ) {
    this.name = name;
    this.bucket = bucket;
    this.region = region;
    this.dualstackEndpointEnabled = dualstackEndpointEnabled;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.endpointOverride = endpointOverride;
  }

  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull String getBucket() {
    return this.bucket;
  }

  public @NotNull String getRegion() {
    return this.region;
  }

  public boolean isDualstackEndpointEnabled() {
    return this.dualstackEndpointEnabled;
  }

  public @NotNull String getAccessKey() {
    return this.accessKey;
  }

  public @NotNull String getSecretKey() {
    return this.secretKey;
  }

  public @Nullable URI getEndpointOverride() {
    if (this.endpointOverride != null) {
      try {
        var uri = URI.create(this.endpointOverride);
        // validate the given uri
        if (uri.getScheme() != null) {
          return uri;
        }
        LOGGER.severe("Endpoint override for s3 config must contain a valid scheme: %s", null, this.endpointOverride);
      } catch (IllegalArgumentException exception) {
        LOGGER.severe("Unable to parse uri for s3 endpoint override: %s", null, this.endpointOverride);
      }
    }
    // illegal uri or not given
    return null;
  }
}
