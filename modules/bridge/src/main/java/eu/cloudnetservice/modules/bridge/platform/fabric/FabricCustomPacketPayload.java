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

package eu.cloudnetservice.modules.bridge.platform.fabric;

import lombok.NonNull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// TODO: this compiles but WONT WORK EVER
record FabricCustomPacketPayload(@NonNull ResourceLocation id, byte[] payload) implements CustomPacketPayload {

  private static final StreamCodec<FriendlyByteBuf, FabricCustomPacketPayload> STREAM_CODEC = CustomPacketPayload.codec(
    FabricCustomPacketPayload::write, FabricCustomPacketPayload::new
  );

  private static final Type<FabricCustomPacketPayload> TYPE = CustomPacketPayload.createType("fabric/custom_payload");

  public FabricCustomPacketPayload(FriendlyByteBuf byteBuf) {
    this(byteBuf.readResourceLocation(), byteBuf.readByteArray());
  }

  private void write(@NonNull FriendlyByteBuf buf) {
    buf.writeBytes(this.payload);
  }

  @Override
  public @NonNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
