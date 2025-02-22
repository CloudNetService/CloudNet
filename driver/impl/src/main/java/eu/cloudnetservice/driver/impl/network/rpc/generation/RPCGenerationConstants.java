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

package eu.cloudnetservice.driver.impl.network.rpc.generation;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.ChainableRPC;
import eu.cloudnetservice.driver.network.rpc.RPC;
import eu.cloudnetservice.driver.network.rpc.RPCChain;
import eu.cloudnetservice.driver.network.rpc.RPCExecutable;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import java.lang.classfile.AccessFlags;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.AccessFlag;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Collections of common constants used during every implementation generation process.
 *
 * @since 4.0
 */
final class RPCGenerationConstants {

  // ==== Method Names ====
  static final String MN_BRIDGE_INVOKE = "bridge$rpc$invoke";
  static final String MN_BRIDGE_GET_CHANNEL = "bridge$rpc$target_channel";

  // ==== Field Names ====
  static final String FN_BASE_RPC = "base_rpc";
  static final String FN_RPC_SENDER = "rpc_sender";
  static final String FN_CHANNEL_SUPPLIER = "channel_supplier";

  // ==== Class Descriptors ====
  static final ClassDesc CD_RPC = ClassDesc.of(RPC.class.getName());
  static final ClassDesc CD_SUPPLIER = ClassDesc.of(Supplier.class.getName());
  static final ClassDesc CD_RPC_CHAIN = ClassDesc.of(RPCChain.class.getName());
  static final ClassDesc CD_RPC_SENDER = ClassDesc.of(RPCSender.class.getName());
  static final ClassDesc CD_TYPE_DESC = ClassDesc.of(TypeDescriptor.class.getName());
  static final ClassDesc CD_CHAINABLE_RPC = ClassDesc.of(ChainableRPC.class.getName());
  static final ClassDesc CD_RPC_EXECUTABLE = ClassDesc.of(RPCExecutable.class.getName());
  static final ClassDesc CD_NETWORK_CHANNEL = ClassDesc.of(NetworkChannel.class.getName());
  static final ClassDesc CD_COMPLETABLE_FUTURE = ClassDesc.of(CompletableFuture.class.getName());
  static final ClassDesc CD_INT_INSTANCE_FACTORY = ClassDesc.of(RPCInternalInstanceFactory.class.getName());
  static final ClassDesc CD_INT_FACTORY_SPECIAL_ARG = CD_INT_INSTANCE_FACTORY.nested("SpecialArg");

  // ==== Method Descriptors ====
  static final MethodTypeDesc MTD_RPC_JOIN = MethodTypeDesc.of(CD_RPC_CHAIN, CD_RPC);
  static final MethodTypeDesc MTD_SUPPLIER_GET = MethodTypeDesc.of(ConstantDescs.CD_Object);
  static final MethodTypeDesc MTD_BRIDGE_GET_CHANNEL = MethodTypeDesc.of(CD_NETWORK_CHANNEL);
  static final MethodTypeDesc MTD_NO_ARGS_CONSTRUCTOR = MethodTypeDesc.of(ConstantDescs.CD_void);
  static final MethodTypeDesc MTD_MTD_OF_DESCRIPTOR =
    MethodTypeDesc.of(ConstantDescs.CD_MethodTypeDesc, ConstantDescs.CD_String);
  static final MethodTypeDesc MTD_RPC_INVOKE =
    MethodTypeDesc.of(CD_RPC, ConstantDescs.CD_String, CD_TYPE_DESC, ConstantDescs.CD_Object.arrayType());
  static final MethodTypeDesc MTD_BRIDGE_RPC_INVOKE =
    MethodTypeDesc.of(CD_CHAINABLE_RPC, ConstantDescs.CD_String, CD_TYPE_DESC, ConstantDescs.CD_Object.arrayType());
  static final MethodTypeDesc MTD_INT_INSTANCE_FACTORY_CONSTRUCT =
    MethodTypeDesc.of(ConstantDescs.CD_Object, CD_CHAINABLE_RPC, CD_SUPPLIER, ConstantDescs.CD_Object.arrayType());

  // ==== Access Flag Masks =====
  static final int AFM_FIELD_PF = AccessFlags.ofField(AccessFlag.PRIVATE, AccessFlag.FINAL).flagsMask();

  private RPCGenerationConstants() {
    throw new UnsupportedOperationException();
  }
}
