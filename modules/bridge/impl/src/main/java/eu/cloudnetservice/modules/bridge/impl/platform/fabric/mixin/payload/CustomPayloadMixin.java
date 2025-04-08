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

package eu.cloudnetservice.modules.bridge.impl.platform.fabric.mixin.payload;

import eu.cloudnetservice.modules.bridge.impl.platform.fabric.access.CustomPayloadAccessor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DiscardedPayload.class)
public class CustomPayloadMixin implements CustomPayloadAccessor {

  @Shadow
  @Final
  private ResourceLocation id;
  @Unique
  public ByteBuf cloudnet_bridge$dataBuf;

  /**
   * Overwrites the codec implementation of the custom packet payload.
   *
   * @author CloudNetService
   * @reason the usual implementation does not allow to send any custom payload data to the client anymore. This reverts
   * that change.
   */
  @Overwrite
  public static <T extends FriendlyByteBuf> StreamCodec<T, DiscardedPayload> codec(ResourceLocation id, int maxBytes) {
    return CustomPacketPayload.codec((discardedPayload, packetSerializer) -> {
      var byteBuf = ((CustomPayloadAccessor) (Object) discardedPayload).cloudnet_bridge$getData();
      try {
        packetSerializer.writeBytes(byteBuf);
      } finally {
        byteBuf.release();
      }
    }, (packetSerializer) -> {
      int readableBytes = packetSerializer.readableBytes();

      if (readableBytes >= 0 && readableBytes <= maxBytes) {
        var payload = new DiscardedPayload(id);
        ((CustomPayloadAccessor) (Object) payload).cloudnet_bridge$setData(packetSerializer.readBytes(readableBytes));
        return payload;
      } else {
        throw new IllegalArgumentException("Payload may not be larger than " + maxBytes + " bytes");
      }
    });
  }

  @Override
  public void cloudnet_bridge$setData(ByteBuf data) {
    this.cloudnet_bridge$dataBuf = data;
  }

  @Override
  public ByteBuf cloudnet_bridge$getData() {
    return this.cloudnet_bridge$dataBuf;
  }
}
