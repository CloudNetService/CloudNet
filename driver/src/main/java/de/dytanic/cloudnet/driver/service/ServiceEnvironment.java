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

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.INameable;
import lombok.NonNull;

public class ServiceEnvironment implements INameable, Cloneable {

  private final String name;
  private final String environmentType;

  protected ServiceEnvironment(@NonNull String name, @NonNull String environmentType) {
    this.name = name;
    this.environmentType = environmentType;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ServiceEnvironment environment) {
    return builder().name(environment.name()).environmentType(environment.environmentType());
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  public @NonNull String environmentType() {
    return this.environmentType;
  }

  @Override
  public ServiceEnvironment clone() {
    try {
      return (ServiceEnvironment) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  public static class Builder {

    private String name;
    private String environmentType;

    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public @NonNull Builder environmentType(@NonNull String environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    public @NonNull Builder environmentType(@NonNull ServiceEnvironmentType type) {
      this.environmentType = type.name();
      return this;
    }

    public @NonNull ServiceEnvironment build() {
      Verify.verifyNotNull(this.name, "no name given");
      Verify.verifyNotNull(this.environmentType, "no environment type given");

      return new ServiceEnvironment(this.name, this.environmentType);
    }
  }
}
