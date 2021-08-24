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

package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.command.CommandInfo;
import org.junit.Assert;
import org.junit.Test;

public class CommandInfoSerializerTest {

  @Test
  public void serializeCommandInfo() {
    CommandInfo original = new CommandInfo(new String[]{"help", "?"}, null, "desc", "any usage");

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    CommandInfo deserialized = buffer.readObject(CommandInfo.class);

    Assert.assertEquals(original, deserialized);
  }

}
