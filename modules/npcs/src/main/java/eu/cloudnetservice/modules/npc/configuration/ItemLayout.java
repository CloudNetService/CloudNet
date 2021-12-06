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

package eu.cloudnetservice.modules.npc.configuration;

import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ItemLayout {

  private final String material;
  private final int subId;

  private final String displayName;
  private final List<String> lore;

  protected ItemLayout(String material, int subId, String displayName, List<String> lore) {
    this.material = material;
    this.subId = subId;
    this.displayName = displayName;
    this.lore = lore;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static @NotNull Builder builder(@NotNull ItemLayout layout) {
    return builder()
      .material(layout.getMaterial())
      .subId(layout.getSubId())
      .displayName(layout.getDisplayName())
      .lore(layout.getLore());
  }

  public @NotNull String getMaterial() {
    return this.material;
  }

  public int getSubId() {
    return this.subId;
  }

  public @NotNull String getDisplayName() {
    return this.displayName;
  }

  public @NotNull List<String> getLore() {
    return this.lore;
  }

  public static class Builder {

    private String material;
    private int subId = -1;
    private String displayName;
    private List<String> lore = new ArrayList<>();

    public Builder material(@NotNull String material) {
      this.material = material;
      return this;
    }

    public Builder subId(int subId) {
      this.subId = subId;
      return this;
    }

    public Builder displayName(@NotNull String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder lore(@NotNull List<String> lore) {
      this.lore = new ArrayList<>(lore);
      return this;
    }

    public ItemLayout build() {
      Verify.verifyNotNull(this.material, "no material given");
      Verify.verifyNotNull(this.displayName, "no display name given");

      return new ItemLayout(material, subId, displayName, lore);
    }
  }
}
