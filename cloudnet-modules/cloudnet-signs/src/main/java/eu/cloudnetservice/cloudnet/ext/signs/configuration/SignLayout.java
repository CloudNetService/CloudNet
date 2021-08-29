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

package eu.cloudnetservice.cloudnet.ext.signs.configuration;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignLayout {

  protected String[] lines;
  protected String blockMaterial;
  protected int blockSubId;
  protected String glowingColor;

  public SignLayout() {
  }

  public SignLayout(String[] lines, String blockMaterial, int blockSubId) {
    this.lines = lines;
    this.blockMaterial = blockMaterial;
    this.blockSubId = blockSubId;
  }

  public String[] getLines() {
    return this.lines;
  }

  public void setLines(String[] lines) {
    this.lines = lines;
  }

  public String getBlockMaterial() {
    return this.blockMaterial;
  }

  public void setBlockMaterial(String blockMaterial) {
    this.blockMaterial = blockMaterial;
  }

  public int getBlockSubId() {
    return this.blockSubId;
  }

  public void setBlockSubId(int blockSubId) {
    this.blockSubId = blockSubId;
  }

  public String getGlowingColor() {
    return this.glowingColor;
  }

  public void setGlowingColor(String glowingColor) {
    this.glowingColor = glowingColor;
  }
}
