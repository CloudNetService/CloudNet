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

package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.util.ArrayList;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the location of a template for services that can either be copied into a service or filled from a service by
 * using a {@link ServiceDeployment}. CloudNet's default storage is "local".
 */
@EqualsAndHashCode
public class ServiceTemplate implements INameable, SerializableObject {

  public static final String LOCAL_STORAGE = "local";

  private String prefix;
  private String name;
  private String storage;

  private boolean alwaysCopyToStaticServices;

  public ServiceTemplate(String prefix, String name, String storage) {
    Preconditions.checkNotNull(prefix);
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(storage);

    this.prefix = prefix;
    this.name = name;
    this.storage = storage;
  }

  public ServiceTemplate(String prefix, String name, String storage, boolean alwaysCopyToStaticServices) {
    this(prefix, name, storage);
    this.alwaysCopyToStaticServices = alwaysCopyToStaticServices;
  }

  public ServiceTemplate() {
  }

  public static ServiceTemplate local(String prefix, String name) {
    return new ServiceTemplate(prefix, name, LOCAL_STORAGE);
  }

  /**
   * Parses a template out of a string in the following format: storage:prefix/name "storage:" is optional, only
   * "prefix/name" needs to be provided
   * <p>
   * {@code alwaysCopyToStaticServices} will always be false in the returned {@link ServiceTemplate}.
   *
   * @param template the template in the specified format
   * @return the parsed {@link ServiceTemplate} or null if the format was invalid
   */
  public static ServiceTemplate parse(String template) {
    String[] base = template.split(":");

    if (base.length > 2) {
      return null;
    }

    String path = base.length == 2 ? base[1] : base[0];
    String storage = base.length == 2 ? base[0] : "local";
    String[] splitPath = path.split("/");

    if (splitPath.length != 2) {
      return null;
    }

    return new ServiceTemplate(splitPath[0], splitPath[1], storage);
  }

  /**
   * Parses multiple templates out of a string in the format specified for {@link #parse(String)} split by ";".
   *
   * @param templates the templates in the specified format
   * @return an array of the parsed templates, this will not contain any null elements if any format is wrong
   */
  @NotNull
  public static ServiceTemplate[] parseArray(String templates) {
    Collection<ServiceTemplate> result = new ArrayList<>();
    for (String template : templates.split(";")) {
      ServiceTemplate serviceTemplate = parse(template);
      if (serviceTemplate != null) {
        result.add(serviceTemplate);
      }
    }
    return result.toArray(new ServiceTemplate[0]);
  }

  public boolean shouldAlwaysCopyToStaticServices() {
    return this.alwaysCopyToStaticServices;
  }

  @Override
  public String toString() {
    return this.storage + ":" + this.prefix + "/" + this.name;
  }

  public String getTemplatePath() {
    return this.prefix + "/" + this.name;
  }

  public String getPrefix() {
    return this.prefix;
  }

  public String getName() {
    return this.name;
  }

  public String getStorage() {
    return this.storage;
  }

  /**
   * Creates a new {@link SpecificTemplateStorage} for this template.
   *
   * @return a new instance of the {@link SpecificTemplateStorage}
   * @throws IllegalArgumentException if the storage in this template doesn't exist
   */
  @NotNull
  public SpecificTemplateStorage storage() {
    return SpecificTemplateStorage.of(this);
  }

  /**
   * Creates a new {@link SpecificTemplateStorage} for the given template.
   *
   * @return a new instance of the {@link SpecificTemplateStorage} or null if the storage doesn't exist
   */
  @Nullable
  public SpecificTemplateStorage nullableStorage() {
    TemplateStorage storage = CloudNetDriver.getInstance().getTemplateStorage(this.storage);
    return storage != null ? SpecificTemplateStorage.of(this, storage) : null;
  }

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeString(this.prefix);
    buffer.writeString(this.name);
    buffer.writeString(this.storage);
    buffer.writeBoolean(this.alwaysCopyToStaticServices);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.prefix = buffer.readString();
    this.name = buffer.readString();
    this.storage = buffer.readString();
    this.alwaysCopyToStaticServices = buffer.readBoolean();
  }
}
