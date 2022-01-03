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
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PermissionUserGroupInfo extends JsonDocPropertyHolder {

  private final String group;
  private final long timeOutMillis;

  protected PermissionUserGroupInfo(@NonNull String group, long timeOutMillis, @NonNull JsonDocument properties) {
    this.group = group;
    this.timeOutMillis = timeOutMillis;
    this.properties = properties;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull PermissionUserGroupInfo info) {
    return builder().group(info.group()).timeOutMillis(info.timeOutMillis()).properties(info.properties());
  }

  public @NonNull String group() {
    return this.group;
  }

  public long timeOutMillis() {
    return this.timeOutMillis;
  }

  public static final class Builder {

    private String group;
    private long timeOutMillis = 0;
    private JsonDocument properties = JsonDocument.newDocument();

    public @NonNull Builder group(@NonNull String group) {
      this.group = group;
      return this;
    }

    public @NonNull Builder timeOutMillis(long timeOutMillis) {
      this.timeOutMillis = timeOutMillis;
      return this;
    }

    public @NonNull Builder timeOut(long timeout, @NonNull TimeUnit unit) {
      this.timeOutMillis = unit.toMillis(timeout);
      return this;
    }

    public @NonNull Builder timeOut(@NonNull Duration duration) {
      this.timeOutMillis = System.currentTimeMillis() + duration.toMillis();
      return this;
    }

    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties;
      return this;
    }

    public @NonNull PermissionUserGroupInfo build() {
      Verify.verifyNotNull(this.group, "Group must be given");
      return new PermissionUserGroupInfo(this.group, this.timeOutMillis, this.properties);
    }
  }
}
