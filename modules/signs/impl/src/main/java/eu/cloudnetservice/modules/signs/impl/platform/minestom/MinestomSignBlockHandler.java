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

package eu.cloudnetservice.modules.signs.impl.platform.minestom;

import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

final class MinestomSignBlockHandler implements BlockHandler {

  public static final MinestomSignBlockHandler SIGN_BLOCK_HANDLER = new MinestomSignBlockHandler();

  private static final Key SIGN_NAMESPACE = Key.key("minecraft", "sign");
  private static final List<Tag<?>> SIGN_ENTITY_TAGS = List.of(
    Tag.Byte("is_waxed"),
    Tag.NBT("front_text"),
    Tag.NBT("back_text"));

  private MinestomSignBlockHandler() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Key getKey() {
    return SIGN_NAMESPACE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Collection<Tag<?>> getBlockEntityTags() {
    return SIGN_ENTITY_TAGS;
  }
}
