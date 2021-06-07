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

package de.dytanic.cloudnet.ext.signs.configuration;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntry;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SignConfiguration {

  public static final Type TYPE = new TypeToken<SignConfiguration>() {
  }.getType();

  protected Collection<SignConfigurationEntry> configurations;

  protected Map<String, String> messages;

  public SignConfiguration(Collection<SignConfigurationEntry> configurations, Map<String, String> messages) {
    this.configurations = configurations;
    this.messages = messages;
  }

  public SignConfiguration() {
  }

  public Collection<SignConfigurationEntry> getConfigurations() {
    return this.configurations;
  }

  public void setConfigurations(Collection<SignConfigurationEntry> configurations) {
    this.configurations = configurations;
  }

  public Map<String, String> getMessages() {
    return this.messages;
  }

  public void setMessages(Map<String, String> messages) {
    this.messages = messages;
  }

}
