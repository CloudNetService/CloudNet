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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record SignLayout(
  @NonNull List<String> lines,
  @NonNull String blockMaterial,
  int blockSubId,
  @Nullable String glowingColor
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull SignLayout signLayout) {
    return builder()
      .lines(new ArrayList<>(signLayout.lines()))
      .blockMaterial(signLayout.blockMaterial())
      .blockSubId(signLayout.blockSubId())
      .glowingColor(signLayout.glowingColor());
  }

  public static class Builder {

    private List<String> lines;
    private String blockMaterial;
    private int blockSubId = -1;
    private String glowingColor;

    public @NonNull Builder lines(@NonNull Collection<String> lines) {
      this.lines = new ArrayList<>(lines);
      return this;
    }

    public @NonNull Builder lines(@NonNull String... lines) {
      return this.lines(List.of(lines));
    }

    public @NonNull Builder blockMaterial(@NonNull String blockMaterial) {
      this.blockMaterial = blockMaterial;
      return this;
    }

    public @NonNull Builder blockSubId(int blockSubId) {
      this.blockSubId = blockSubId;
      return this;
    }

    public @NonNull Builder glowingColor(@Nullable String glowingColor) {
      this.glowingColor = glowingColor;
      return this;
    }

    public @NonNull SignLayout build() {
      Preconditions.checkNotNull(this.lines, "Missing lines");
      Preconditions.checkNotNull(this.blockMaterial, "Missing block material");

      return new SignLayout(this.lines, this.blockMaterial, this.blockSubId, this.glowingColor);
    }
  }

}
