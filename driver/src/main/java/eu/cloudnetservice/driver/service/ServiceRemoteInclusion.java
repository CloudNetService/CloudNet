/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.common.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.common.document.property.DocProperty;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * An inclusion which can be added to a service and will download a file from the specified url to the given
 * destination. This can be anything for example a plugin or configuration file.
 *
 * @since 4.0
 */
@EqualsAndHashCode
public final class ServiceRemoteInclusion
  implements DefaultedDocPropertyHolder<JsonDocument, ServiceRemoteInclusion>, Cloneable {

  /**
   * A property which can be added to a service inclusion to set the http headers to send when making the download http
   * request. All key-value pairs of the given document will be set as headers in the request.
   */
  public static final DocProperty<JsonDocument> HEADERS = DocProperty.property("headers", JsonDocument.class);

  private final String url;
  private final String destination;
  private final JsonDocument properties;

  /**
   * Constructs a new service remote inclusion instance.
   *
   * @param url         the url to download the associated file from.
   * @param destination the destination inside the service directory to copy the downloaded file to.
   * @param properties  the properties of the remote inclusion, these can for example contain the http headers to send.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private ServiceRemoteInclusion(@NonNull String url, @NonNull String destination, @NonNull JsonDocument properties) {
    this.url = url;
    this.destination = destination;
    this.properties = properties;
  }

  /**
   * Constructs a new builder instance for a service remote inclusion.
   *
   * @return a new service remote inclusion builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder for a service remote inclusion which has the properties of the given inclusion already
   * set.
   * <p>
   * When calling build directly after constructing a builder using this method, it will result in a service remote
   * inclusion which is equal but not the same as the given one.
   *
   * @param inclusion the inclusion to copy the properties of.
   * @return a new service remote inclusion builder with the properties of the given one already set.
   * @throws NullPointerException if the given inclusion is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceRemoteInclusion inclusion) {
    return builder()
      .url(inclusion.url())
      .destination(inclusion.destination())
      .properties(inclusion.propertyHolder());
  }

  /**
   * Get the url to download the remote inclusion from. The only supported schemes are http and https by default.
   *
   * @return the download url of the remote inclusion.
   */
  public @NonNull String url() {
    return this.url;
  }

  /**
   * The target file name of the inclusion download. An inclusion destination which is outside the service directory
   * will result in an exception.
   *
   * @return the destination of the downloaded inclusion file.
   */
  public @NonNull String destination() {
    return this.destination;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull JsonDocument propertyHolder() {
    return this.properties;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.url + ':' + this.destination;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ServiceRemoteInclusion clone() {
    try {
      return (ServiceRemoteInclusion) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * A builder for a service remote inclusion.
   *
   * @since 4.0
   */
  public static class Builder {

    protected String url;
    protected String destination;
    protected JsonDocument properties = JsonDocument.newDocument();

    /**
     * Sets the download url of the inclusion. The url scheme should be http/https (the only supported ones by default)
     * but no check is made in this method if the scheme is valid and can be loaded. Including the remote inclusions
     * might result in an exception when an invalid scheme was given.
     *
     * @param url the url to download the inclusion from.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given url is null.
     */
    public @NonNull Builder url(@NonNull String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the destination to download the inclusion to. The given path should be relative to the service directory,
     * for example to download into the plugin folder: plugins/BedWars.jar
     *
     * @param destination the service relative destination path to download the inclusion to.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given destination path is null.
     */
    public @NonNull Builder destination(@NonNull String destination) {
      this.destination = destination;
      return this;
    }

    /**
     * Sets the properties of the service remote inclusion. The properties can for example be used to set the http
     * headers which should get send when making a request to the given download url.
     *
     * @param properties the properties of the service inclusion.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given properties document is null.
     */
    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties.clone();
      return this;
    }

    /**
     * Builds a service remote inclusion instance based on this builder.
     *
     * @return the service remote inclusion.
     * @throws NullPointerException if no url or destination was given.
     */
    public @NonNull ServiceRemoteInclusion build() {
      Preconditions.checkNotNull(this.url, "no url given");
      Preconditions.checkNotNull(this.destination, "no destination given");

      return new ServiceRemoteInclusion(this.url, this.destination, this.properties);
    }
  }
}
