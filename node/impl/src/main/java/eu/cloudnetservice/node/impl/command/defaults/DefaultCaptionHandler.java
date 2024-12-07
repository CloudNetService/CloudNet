/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.command.defaults;

import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.command.source.CommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.NonNull;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.CaptionFormatter;
import org.incendo.cloud.caption.CaptionProvider;
import org.incendo.cloud.caption.CaptionVariable;

@Singleton
final class DefaultCaptionHandler implements CaptionProvider<CommandSource>, CaptionFormatter<CommandSource, String> {

  private final I18n i18n;

  @Inject
  DefaultCaptionHandler(@NonNull @Service I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public @NonNull String provide(@NonNull Caption caption, @NonNull CommandSource recipient) {
    return caption.key().replace('.', '-').replace('_', '-');
  }

  @Override
  @NonNull
  public String formatCaption(
    @NonNull Caption captionKey,
    @NonNull CommandSource recipient,
    @NonNull String caption,
    @NonNull List<CaptionVariable> variables
  ) {
    return this.i18n.translate(caption, variables.stream().map(CaptionVariable::value).toArray());
  }
}
