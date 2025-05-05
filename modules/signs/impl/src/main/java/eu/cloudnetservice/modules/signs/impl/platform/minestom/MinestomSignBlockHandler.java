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

package eu.cloudnetservice.modules.signs.impl.platform.minestom;

import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;

final class MinestomSignBlockHandler implements BlockHandler {

  public static final MinestomSignBlockHandler SIGN_BLOCK_HANDLER = new MinestomSignBlockHandler();
  private static final List<Tag<?>> ENTITY_TAGS = List.of(
    Tag.Byte("GlowingText"),
    Tag.String("Color"),
    Tag.String("Text1"),
    Tag.String("Text2"),
    Tag.String("Text3"),
    Tag.String("Text4"));

  private static final NamespaceID SIGN_NAMESPACE = NamespaceID.from("minecraft:sign");

  private MinestomSignBlockHandler() {
  }

  @Override
  public @NonNull Collection<Tag<?>> getBlockEntityTags() {
    return ENTITY_TAGS;
  }

  @Override
  public @NonNull NamespaceID getNamespaceId() {
    return SIGN_NAMESPACE;
  }
}
