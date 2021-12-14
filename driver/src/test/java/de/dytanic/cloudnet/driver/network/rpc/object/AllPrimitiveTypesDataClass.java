/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.object;

import java.util.Objects;

public final class AllPrimitiveTypesDataClass {

  private final byte b;
  private final short s;
  private final int i;
  private final long l;
  private final float f;
  private final double d;
  private final char c;
  private final String string;
  private final boolean bol;

  public AllPrimitiveTypesDataClass() {
    this((byte) 1, (short) 2, 3, 4L, 5F, 6D, '/', "Hello, World!", true);
  }

  public AllPrimitiveTypesDataClass(
    byte b,
    short s,
    int i,
    long l,
    float f,
    double d,
    char c,
    String st,
    boolean bol
  ) {
    this.b = b;
    this.s = s;
    this.i = i;
    this.l = l;
    this.f = f;
    this.d = d;
    this.c = c;
    this.string = st;
    this.bol = bol;
  }

  public byte getB() {
    return this.b;
  }

  public short getS() {
    return this.s;
  }

  public int getI() {
    return this.i;
  }

  public long getL() {
    return this.l;
  }

  public float getF() {
    return this.f;
  }

  public double getD() {
    return this.d;
  }

  public char getC() {
    return this.c;
  }

  public String getString() {
    return this.string;
  }

  public boolean isBol() {
    return this.bol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AllPrimitiveTypesDataClass that)) {
      return false;
    }
    return this.getB() == that.getB()
      && this.getS() == that.getS()
      && this.getI() == that.getI()
      && this.getL() == that.getL()
      && Float.compare(that.getF(), this.getF()) == 0
      && Double.compare(that.getD(), this.getD()) == 0
      && this.getC() == that.getC()
      && this.isBol() == that.isBol()
      && Objects.equals(this.getString(), that.getString());
  }
}
