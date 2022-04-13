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

package eu.cloudnetservice.node.command.defaults;

import cloud.commandframework.captions.Caption;
import cloud.commandframework.captions.CaptionRegistry;
import cloud.commandframework.captions.StandardCaptionKeys;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.node.command.source.CommandSource;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

public class I18nCaptionRegistry implements CaptionRegistry<CommandSource> {

  private final Map<Caption, String> messages = new HashMap<>();

  public I18nCaptionRegistry() {
    for (var caption : StandardCaptionKeys.getStandardCaptionKeys()) {
      var key = caption.getKey().replace('.', '-').replace('_', '-');
      this.messages.put(caption, I18n.trans(key));
    }
  }

  @Override
  public @NonNull String getCaption(@NonNull Caption caption, @NonNull CommandSource sender) {
    var message = this.messages.get(caption);
    if (message == null) {
      throw new IllegalArgumentException("There is no translation for " + caption.getKey());
    }
    return this.messages.get(caption);
  }
}
