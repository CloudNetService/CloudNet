/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.ext.bungee;

import java.util.ArrayList;
import java.util.regex.Pattern;
import lombok.NonNull;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public final class BungeeComponentUtil {

  private BungeeComponentUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Replaces all occurrences of the given pattern in the given component and its children with the given replacement.
   *
   * @param component   the component to replace the occurrences of the given pattern in.
   * @param pattern     the pattern to find and replace in the given component.
   * @param replacement the replacement to use for the pattern in the component.
   * @return a component with all occurrences of the given pattern replaced.
   * @throws NullPointerException if the given component, matcher or replacement is null.
   */
  public static @NonNull BaseComponent replaceText(
    @NonNull BaseComponent component,
    @NonNull Pattern pattern,
    @NonNull BaseComponent replacement
  ) {
    var modifiedComponent = component;
    var oldExtra = component.getExtra();
    var newExtra = new ArrayList<BaseComponent>();

    if (component instanceof TextComponent textComponent) {
      var lastReplacedIndex = 0;
      var text = textComponent.getText();
      var matcher = pattern.matcher(text);
      while (matcher.find()) {
        if (matcher.start() == 0) {
          if (matcher.end() == text.length()) {
            // the component is a full match against the given pattern, we can
            // just replace the current component with the replacement component
            modifiedComponent = replacement.duplicate();
            modifiedComponent.copyFormatting(component);

            var replacementExtra = replacement.getExtra();
            if (replacementExtra != null && !replacementExtra.isEmpty()) {
              newExtra.addAll(replacement.getExtra());
            }
          } else {
            // not a full match, but because the match is at the first index, we
            // just use an empty component as the replacement and add the replacement
            // component as its first extra component
            modifiedComponent = new TextComponent();
            newExtra.add(replacement);
          }
        } else {
          if (modifiedComponent == component) {
            // first match, initialize the target component & add
            // the text up until the first match into the component
            modifiedComponent = component.duplicate();
            var prefixText = text.substring(0, matcher.start());
            ((TextComponent) modifiedComponent).setText(prefixText);
          } else if (lastReplacedIndex < matcher.start()) {
            // not the first match, add the literal text between the last match
            // and the current match as an extra of the new component
            var middleText = text.substring(lastReplacedIndex, matcher.start());
            newExtra.add(new TextComponent(middleText));
          }

          // add the replacement for the current matched token as an
          // extra of the new component we're building
          newExtra.add(replacement.duplicate());
        }

        lastReplacedIndex = matcher.end();
      }

      if (lastReplacedIndex > 0 && lastReplacedIndex < text.length()) {
        // copy over the trailing text from the original component
        var trailingText = text.substring(lastReplacedIndex);
        newExtra.add(new TextComponent(trailingText));
      }
    }

    // replace the text in all extra components of the current component
    if (oldExtra != null && !oldExtra.isEmpty()) {
      for (var extra : oldExtra) {
        var replaced = replaceText(extra, pattern, replacement);
        newExtra.add(replaced);
      }
    }

    // only set extra components if we really constructed some, bungee only
    // checks if extra is null during serialization, not if the list is empty
    // which results in a decoding error on the client later on
    if (!newExtra.isEmpty()) {
      modifiedComponent.setExtra(newExtra);
    }

    return modifiedComponent;
  }
}
