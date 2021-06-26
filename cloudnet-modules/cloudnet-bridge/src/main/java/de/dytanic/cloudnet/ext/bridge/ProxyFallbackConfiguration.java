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

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ProxyFallbackConfiguration {

  protected String targetGroup;
  protected String defaultFallbackTask;

  protected List<ProxyFallback> fallbacks;

  public ProxyFallbackConfiguration(String targetGroup, String defaultFallbackTask, List<ProxyFallback> fallbacks) {
    this.targetGroup = targetGroup;
    this.defaultFallbackTask = defaultFallbackTask;
    this.fallbacks = fallbacks;
  }

  public String getTargetGroup() {
    return this.targetGroup;
  }

  public void setTargetGroup(String targetGroup) {
    this.targetGroup = targetGroup;
  }

  public String getDefaultFallbackTask() {
    return this.defaultFallbackTask;
  }

  public void setDefaultFallbackTask(String defaultFallbackTask) {
    this.defaultFallbackTask = defaultFallbackTask;
  }

  public List<ProxyFallback> getFallbacks() {
    return this.fallbacks;
  }

  public void setFallbacks(List<ProxyFallback> fallbacks) {
    this.fallbacks = fallbacks;
  }

}
