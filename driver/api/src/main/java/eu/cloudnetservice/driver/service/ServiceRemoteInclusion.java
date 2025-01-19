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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.document.property.DocProperty;
import io.leangen.geantyref.TypeFactory;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An inclusion which can be added to a service and will download a file from the specified url to the given
 * destination. This can be anything for example a plugin or configuration file.
 *
 * @since 4.0
 */
public final class ServiceRemoteInclusion implements DefaultedDocPropertyHolder, Cloneable {

  public static final String NO_CACHE_STRATEGY = "none";
  public static final String KEEP_UNTIL_RESTART_STRATEGY = "until-node-restart";

  /**
   * A property which can be added to a service inclusion to set the http headers to send when making the download http
   * request. All key-value pairs of the given document will be set as headers in the request.
   */
  public static final DocProperty<Map<String, String>> HEADERS = DocProperty.genericProperty(
    "headers",
    TypeFactory.parameterizedClass(Map.class, String.class, String.class));

  private final String url;
  private final String destination;
  private final String cacheStrategy;
  private final Document properties;

  /**
   * Constructs a new service remote inclusion instance.
   *
   * @param url           the url to download the associated file from.
   * @param destination   the destination inside the service directory to copy the downloaded file to.
   * @param cacheStrategy the cache strategy to use when downloading files from the remote.
   * @param properties    the properties of the remote inclusion, these can for example contain the http headers to
   *                      send.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private ServiceRemoteInclusion(
    @NonNull String url,
    @NonNull String destination,
    @NonNull String cacheStrategy,
    @NonNull Document properties
  ) {
    this.url = url;
    this.destination = destination;
    this.cacheStrategy = cacheStrategy;
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
      .cacheStrategy(inclusion.cacheStrategy())
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
   * The caching strategy that is used when downloading the inclusion.
   *
   * @return the caching strategy.
   */
  public @NonNull String cacheStrategy() {
    return this.cacheStrategy;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
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
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ServiceRemoteInclusion that)) {
      return false;
    }
    return Objects.equals(this.url, that.url()) && Objects.equals(this.destination, that.destination());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.url, this.destination);
  }

  /**
   * A builder for a service remote inclusion.
   *
   * @since 4.0
   */
  public static class Builder {

    protected String url;
    protected String destination;
    protected String cacheStrategy = ServiceRemoteInclusion.NO_CACHE_STRATEGY;
    protected Document properties = Document.emptyDocument();

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
     * Sets the cache strategy that is used when downloading the inclusion from the remote.
     * <p>
     * To disable caching use the {@link ServiceRemoteInclusion#NO_CACHE_STRATEGY}, which is also the default of this
     * builder.
     *
     * @param cacheStrategy the caching strategy to use.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given cache strategy is null.
     */
    public @NonNull Builder cacheStrategy(@NonNull String cacheStrategy) {
      this.cacheStrategy = cacheStrategy;
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
    public @NonNull Builder properties(@NonNull Document properties) {
      this.properties = properties.immutableCopy();
      return this;
    }

    /**
     * Builds a service remote inclusion instance based on this builder.
     *
     * @return the service remote inclusion.
     * @throws NullPointerException if no url, destination or cache strategy is given.
     */
    public @NonNull ServiceRemoteInclusion build() {
      Preconditions.checkNotNull(this.url, "no url given");
      Preconditions.checkNotNull(this.destination, "no destination given");
      Preconditions.checkNotNull(this.cacheStrategy, "no cacheStrategy given");

      return new ServiceRemoteInclusion(this.url, this.destination, this.cacheStrategy, this.properties);
    }
  }
}
