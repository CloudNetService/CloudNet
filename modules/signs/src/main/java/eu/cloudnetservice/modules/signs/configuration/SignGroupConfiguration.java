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

package eu.cloudnetservice.modules.signs.configuration;

import com.google.common.base.Verify;
import lombok.NonNull;

public record SignGroupConfiguration(
  @NonNull String targetGroup,
  boolean switchToSearchingWhenServiceIsFull,
  @NonNull SignLayoutsHolder emptyLayout,
  @NonNull SignLayoutsHolder onlineLayout,
  @NonNull SignLayoutsHolder fullLayout) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SignGroupConfiguration groupConfiguration) {
    return builder()
      .targetGroup(groupConfiguration.targetGroup())
      .switchToSearchingWhenServiceIsFull(groupConfiguration.switchToSearchingWhenServiceIsFull())
      .emptyLayout(groupConfiguration.emptyLayout())
      .onlineLayout(groupConfiguration.onlineLayout())
      .fullLayout(groupConfiguration.fullLayout());
  }

  public static class Builder {

    private String targetGroup;
    private boolean switchToSearchingWhenServiceIsFull = false;
    private SignLayoutsHolder emptyLayout;
    private SignLayoutsHolder onlineLayout;
    private SignLayoutsHolder fullLayout;

    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    public @NonNull Builder switchToSearchingWhenServiceIsFull(boolean switchToSearchingWhenServiceIsFull) {
      this.switchToSearchingWhenServiceIsFull = switchToSearchingWhenServiceIsFull;
      return this;
    }

    public @NonNull Builder emptyLayout(@NonNull SignLayoutsHolder emptyLayout) {
      this.emptyLayout = emptyLayout;
      return this;
    }

    public @NonNull Builder onlineLayout(@NonNull SignLayoutsHolder onlineLayout) {
      this.onlineLayout = onlineLayout;
      return this;
    }

    public @NonNull Builder fullLayout(@NonNull SignLayoutsHolder fullLayout) {
      this.fullLayout = fullLayout;
      return this;
    }

    public @NonNull SignGroupConfiguration build() {
      Verify.verifyNotNull(this.targetGroup, "Missing target group");
      Verify.verifyNotNull(this.emptyLayout, "Missing empty sign layout");
      Verify.verifyNotNull(this.onlineLayout, "Missing online sign layout");
      Verify.verifyNotNull(this.fullLayout, "Missing full sign layout");

      return new SignGroupConfiguration(
        this.targetGroup,
        this.switchToSearchingWhenServiceIsFull,
        this.emptyLayout,
        this.onlineLayout,
        this.fullLayout);
    }
  }
}
