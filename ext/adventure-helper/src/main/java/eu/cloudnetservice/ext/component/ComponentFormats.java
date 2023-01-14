/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.component;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;

public final class ComponentFormats {

  public static final ComponentFormat<Component> ADVENTURE = new AdventureComponentFormat();
  public static final ComponentFormat<BaseComponent[]> BUNGEE = new BungeeComponentFormat();

  public static final ComponentConverter<BaseComponent[]> BUNGEE_TO_BUNGEE = BUNGEE.converterTo(BUNGEE);
  public static final ComponentConverter<BaseComponent[]> ADVENTURE_TO_BUNGEE = ADVENTURE.converterTo(BUNGEE)
    .andThen(BUNGEE_TO_BUNGEE);

  public static final ComponentConverter<Component> ADVENTURE_TO_ADVENTURE = ADVENTURE.converterTo(ADVENTURE);
  public static final ComponentConverter<Component> BUNGEE_TO_ADVENTURE = BUNGEE.converterTo(ADVENTURE)
    .andThen(ADVENTURE_TO_ADVENTURE);

  private ComponentFormats() {
    throw new UnsupportedOperationException();
  }
}
