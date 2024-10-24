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

package eu.cloudnetservice.driver.impl.network.rpc.generation.api;

import eu.cloudnetservice.driver.network.rpc.annotation.RPCInvocationTarget;

public abstract class TestRPCParameters {

  @RPCInvocationTarget
  public TestRPCParameters(int a, long b, String c, int[] d, long[] e, String[] f) {
  }

  public abstract void testVoid();

  public abstract void testString(String string);

  public abstract void testInt(int i);

  public abstract void testLong(long l);

  public abstract void testFloat(float f);

  public abstract void testDouble(double d);

  public abstract void testBoolean(boolean b);

  public abstract void testChar(char c);

  public abstract void testByte(byte b);

  public abstract void testShort(short s);

  public abstract void testStringArray(String[] strings);

  public abstract void testIntArray(int[] ints);

  public abstract void testLongArray(long[] longs);

  public abstract void testFloatArray(float[] floats);

  public abstract void testDoubleArray(double[] doubles);

  public abstract void testBooleanArray(boolean[] booleans);

  public abstract void testCharArray(char[] chars);

  public abstract void testByteArray(byte[] bytes);

  public abstract void testShortArray(short[] shorts);

  public abstract void testLongInt(long l, int i);

  public abstract void testLongIntStringArray(long l, int i, String[] s);

  public abstract void testLongIntString(long l, int i, String s);

  public abstract void testLongIntStringArray(long l, int i, String s, String[] o);
}
