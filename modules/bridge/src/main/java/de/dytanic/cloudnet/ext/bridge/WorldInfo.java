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

package de.dytanic.cloudnet.ext.bridge;

import java.util.Map;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class WorldInfo {

  protected UUID uniqueId;

  protected String name;

  protected String difficulty;

  protected Map<String, String> gameRules;

  public WorldInfo(UUID uniqueId, String name, String difficulty, Map<String, String> gameRules) {
    this.uniqueId = uniqueId;
    this.name = name;
    this.difficulty = difficulty;
    this.gameRules = gameRules;
  }

  public UUID getUniqueId() {
    return this.uniqueId;
  }

  public void setUniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDifficulty() {
    return this.difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public Map<String, String> getGameRules() {
    return this.gameRules;
  }

  public void setGameRules(Map<String, String> gameRules) {
    this.gameRules = gameRules;
  }

}
