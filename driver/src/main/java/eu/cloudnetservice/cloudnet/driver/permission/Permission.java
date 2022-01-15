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

package eu.cloudnetservice.cloudnet.driver.permission;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.common.Nameable;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

public record Permission(
  @NonNull String name,
  int potency,
  long timeOutMillis
) implements Nameable, Comparable<Permission> {

  public static @NonNull Permission of(@NonNull String permission) {
    return builder().name(permission).build();
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull Permission permission) {
    return builder()
      .name(permission.name())
      .potency(permission.potency())
      .timeOutMillis(permission.timeOutMillis());
  }

  @Override
  public int compareTo(@NonNull Permission o) {
    return Integer.compare(Math.abs(this.potency()), Math.abs(o.potency()));
  }

  public static class Builder {

    private String name;
    private int potency = 0;
    private long timeOutMillis = 0;

    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public @NonNull Builder potency(int potency) {
      this.potency = potency;
      return this;
    }

    public @NonNull Builder timeOutMillis(long timeOutMillis) {
      this.timeOutMillis = timeOutMillis;
      return this;
    }

    public @NonNull Builder timeOutMillis(@NonNull TimeUnit unit, long timeOut) {
      return this.timeOutMillis(System.currentTimeMillis() + unit.toMillis(timeOut));
    }

    public @NonNull Permission build() {
      Verify.verifyNotNull(this.name, "Missing name");

      return new Permission(this.name, this.potency, this.timeOutMillis);
    }
  }
}
