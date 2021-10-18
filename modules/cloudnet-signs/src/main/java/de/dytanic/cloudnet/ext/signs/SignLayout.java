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

package de.dytanic.cloudnet.ext.signs;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignLayout {

  protected String[] lines;

  protected String blockType;

  protected int subId;

  public SignLayout(String[] lines, String blockType, int subId) {
    this.lines = lines;
    this.blockType = blockType;
    this.subId = subId;
  }

  public SignLayout() {
  }

  public String[] getLines() {
    return this.lines;
  }

  public void setLines(String[] lines) {
    this.lines = lines;
  }

  public String getBlockType() {
    return this.blockType;
  }

  public void setBlockType(String blockType) {
    this.blockType = blockType;
  }

  public int getSubId() {
    return this.subId;
  }

  public void setSubId(int subId) {
    this.subId = subId;
  }

}
