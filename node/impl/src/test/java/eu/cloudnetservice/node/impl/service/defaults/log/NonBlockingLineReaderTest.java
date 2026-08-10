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

package eu.cloudnetservice.node.impl.service.defaults.log;

import eu.cloudnetservice.utils.base.StringUtil;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class NonBlockingLineReaderTest {

  @Test
  void testReaderNotMarkedAsReadyIfUnderlyingReaderIsNotReady() throws IOException {
    var source = Mockito.mock(Reader.class);
    Mockito.when(source.ready()).thenReturn(false);

    var reader = new NonBlockingLineReader(source);
    Assertions.assertFalse(reader.ready());
  }

  @Test
  void testReaderStillMarkedAsReadyIfContentIsBuffered() throws IOException {
    var source = new StringReader("Hello\nWorld");
    var reader = new NonBlockingLineReader(source);
    Assertions.assertTrue(reader.ready());
    Assertions.assertEquals("Hello", reader.readLine());
    Assertions.assertDoesNotThrow(reader::close);
    Assertions.assertTrue(reader.ready());
    Assertions.assertEquals("World", reader.readLine());
    Assertions.assertFalse(reader.ready());
  }

  @Test
  void testClosingReaderIsIdempotent() {
    var source = new StringReader("Hello\nWorld");
    var reader = new NonBlockingLineReader(source);

    Assertions.assertDoesNotThrow(reader::close);
    Assertions.assertDoesNotThrow(reader::close);
  }

  @Test
  void testMultipleNewlinesAreParsedCorrectly() throws IOException {
    var source = new StringReader("Hello\n\nWorld\n");
    var reader = new NonBlockingLineReader(source);
    Assertions.assertTrue(reader.ready());
    Assertions.assertEquals("Hello", reader.readLine());
    Assertions.assertEquals("", reader.readLine());
    Assertions.assertEquals("World", reader.readLine());
    Assertions.assertTrue(reader.ready()); // no lf at end of line and reader is still open
    Assertions.assertNull(reader.readLine());
  }

  @Test
  void testLfOnlyInput() throws IOException {
    var source = new StringReader("\n");
    var reader = new NonBlockingLineReader(source);
    Assertions.assertTrue(reader.ready());
    Assertions.assertEquals("", reader.readLine());
  }

  @Test
  void testCrLfInputSplitting() throws IOException {
    var source = new StringReader("Hello\r\nWorld\n");
    var reader = new NonBlockingLineReader(source);
    Assertions.assertTrue(reader.ready());
    Assertions.assertEquals("Hello", reader.readLine());
    Assertions.assertEquals("World", reader.readLine());
    Assertions.assertNull(reader.readLine());
  }

  @Test
  void testCrLfOnlyInput() throws IOException {
    var source = new StringReader("\r\n");
    var reader = new NonBlockingLineReader(source);
    Assertions.assertTrue(reader.ready());
    Assertions.assertEquals("", reader.readLine());
    Assertions.assertNull(reader.readLine());
  }

  @Test
  void testToStringOnBufferOverflow() throws IOException {
    var longLine = StringUtil.generateRandomString(9000);
    var source = new StringReader(longLine + "\n");
    var reader = new NonBlockingLineReader(source);

    // some runs, buffer needs to fill first (1024 chars per readLine call)
    for (var run = 0; run < 10; run++) {
      var line = reader.readLine();
      if (line != null) {
        var nextLine = reader.readLine();
        Assertions.assertEquals(8192, line.length());
        Assertions.assertNotNull(nextLine);
        Assertions.assertEquals(808, nextLine.length()); // remaining chars of the 9000
        Assertions.assertNull(reader.readLine());
        return;
      }
    }

    Assertions.fail("Didn't convert buffer to line after 8192 chars");
  }
}
