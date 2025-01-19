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

package eu.cloudnetservice.driver.cluster;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.network.HostAndPort;
import java.util.List;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a general information holder about a node running in a cluster. Every node knows this information about
 * each node which runs in the cluster, even if the node is not connected.
 *
 * @since 4.0
 */
@ToString
public final class NetworkClusterNode implements DefaultedDocPropertyHolder {

  private final String uniqueId;
  private final List<HostAndPort> listeners;

  private final Document properties;

  /**
   * Creates a new instance of a network cluster node.
   *
   * @param uniqueId  the unique id of the node.
   * @param listeners the listeners of the node which are always running.
   * @throws NullPointerException if either the id or listener array is null.
   */
  public NetworkClusterNode(@NonNull String uniqueId, @NonNull List<HostAndPort> listeners) {
    this(uniqueId, listeners, Document.emptyDocument());
  }

  /**
   * Creates a new instance of a network cluster node.
   *
   * @param uniqueId   the unique id of the node.
   * @param listeners  the listeners of the node which are always running.
   * @param properties the properties which are set for this node, mainly for developers to store information.
   * @throws NullPointerException if either the id or listener array is null.
   */
  public NetworkClusterNode(
    @NonNull String uniqueId,
    @NonNull List<HostAndPort> listeners,
    @NonNull Document properties
  ) {
    this.uniqueId = uniqueId;
    this.listeners = listeners;
    this.properties = properties;
  }

  /**
   * Get the unique id of this node.
   *
   * @return the unique id of this node.
   */
  public @NonNull String uniqueId() {
    return this.uniqueId;
  }

  /**
   * Get all listener of this node.
   *
   * @return all listener of this node.
   */
  public @NonNull List<HostAndPort> listeners() {
    return this.listeners;
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
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof NetworkClusterNode other) {
      return other.uniqueId().equals(this.uniqueId);
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.uniqueId.hashCode();
  }
}
