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

package eu.cloudnetservice.wrapper.event;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import lombok.NonNull;

/**
 * An event that is called when the properties of the service that is associated with the current wrapper instance are
 * prepared. The properties that are appended to this event are copied into the service snapshot that gets published.
 * <p>
 * If you want to receive a notification about the publication of a new service info snapshot instance, listen to the
 * {@link ServiceInfoSnapshotPublishEvent} instead.
 *
 * @since 4.0
 */
public final class ServiceInfoPropertiesConfigureEvent extends Event
  implements DefaultedDocPropertyHolder.Mutable<ServiceInfoPropertiesConfigureEvent> {

  private final Document.Mutable propertyDocument;
  private final ServiceInfoSnapshot lastServiceInfo;

  /**
   * Constructs a new service properties configure event.
   *
   * @param propertyDocument the target document, can be preconfigured with properties.
   * @param lastServiceInfo  the last published service info, for reference.
   * @throws NullPointerException if the given property document or last service info is null.
   */
  public ServiceInfoPropertiesConfigureEvent(
    @NonNull Document.Mutable propertyDocument,
    @NonNull ServiceInfoSnapshot lastServiceInfo
  ) {
    this.propertyDocument = propertyDocument;
    this.lastServiceInfo = lastServiceInfo;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document.Mutable propertyHolder() {
    return this.propertyDocument;
  }

  /**
   * Get the last service info of this wrapper.
   *
   * @return the last service info published by this wrapper.
   */
  public @NonNull ServiceInfoSnapshot lastServiceInfo() {
    return this.lastServiceInfo;
  }
}
