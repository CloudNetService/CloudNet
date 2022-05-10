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

package eu.cloudnetservice.modules.npc.configuration;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;

public record ItemLayout(
  @NonNull String material,
  int subId,
  @NonNull String displayName,
  @NonNull List<String> lore
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ItemLayout layout) {
    return builder()
      .material(layout.material())
      .subId(layout.subId())
      .displayName(layout.displayName())
      .lore(layout.lore());
  }

  public static class Builder {

    private String material;
    private int subId = -1;
    private String displayName;
    private List<String> lore = new ArrayList<>();

    public Builder material(@NonNull String material) {
      this.material = material;
      return this;
    }

    public Builder subId(int subId) {
      this.subId = subId;
      return this;
    }

    public Builder displayName(@NonNull String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder lore(@NonNull List<String> lore) {
      this.lore = new ArrayList<>(lore);
      return this;
    }

    public ItemLayout build() {
      Preconditions.checkNotNull(this.material, "no material given");
      Preconditions.checkNotNull(this.displayName, "no display name given");

      return new ItemLayout(this.material, this.subId, this.displayName, this.lore);
    }
  }
}
