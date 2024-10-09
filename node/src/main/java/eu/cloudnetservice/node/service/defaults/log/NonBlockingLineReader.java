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

package eu.cloudnetservice.node.service.defaults.log;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A reader that can read lines from a given reader without blocking the caller while waiting for a newline.
 *
 * @since 4.0
 */
final class NonBlockingLineReader implements Closeable {

  private static final int CHAR_BUFFER_SIZE = 1024;
  private static final int MAX_STRING_BUFFER_SIZE = 8192;

  private final char[] charBuffer = new char[CHAR_BUFFER_SIZE];
  private final StringBuilder stringBuffer = new StringBuilder();

  private Reader reader;

  /**
   * Constructs a new non-blocking line reader.
   *
   * @param reader the reader to delegate reader operations to.
   * @throws NullPointerException if the given reader is null.
   */
  public NonBlockingLineReader(@NonNull Reader reader) {
    this.reader = reader;
  }

  /**
   * Tries to read a line by reading from the underlying reader, unless this reader was closed. If the reader was closed
   * or no line separator was found in the read content, there are three possibilities:
   * <ol>
   *   <li>The buffer is getting larger than {@link #MAX_STRING_BUFFER_SIZE}, in that case the complete buffered
   *   content is returned to prevent a memory overuse.
   *   <li>This reader was closed, in that case the complete buffered content is returned to allow getting even an
   *   incomplete line from the buffer.
   *   <li>If both the above cases are not met then null is returned.
   * </ol>
   *
   * @return a string containing a line or null, as described above.
   * @throws IOException if an I/O error occurs.
   */
  public @Nullable String readLine() throws IOException {
    // if the buffer is closed, try to read a line
    // from the remaining buffered content
    var reader = this.reader;
    if (reader == null) {
      return this.readLineFromBuffer();
    }

    // if the input is ready read all chars that are available right away
    if (reader.ready()) {
      var charsRead = reader.read(this.charBuffer);
      if (charsRead != -1) {
        this.stringBuffer.append(this.charBuffer, 0, charsRead);
      }
    }

    // try to construct a line from the buffered chars
    return this.readLineFromBuffer();
  }

  /**
   * Tries to find a line separator in the buffered content and constructs a string from the chars up until the line
   * separator. If no line separator is found, there are three possibilities:
   * <ol>
   *   <li>The buffer is getting larger than {@link #MAX_STRING_BUFFER_SIZE}, in that case the complete buffered
   *   content is returned to prevent a memory overuse.
   *   <li>This reader was closed, in that case the complete buffered content is returned to allow getting even an
   *   incomplete line from the buffer.
   *   <li>If both the above cases are not met then null is returned.
   * </ol>
   *
   * @return a string containing a line or null, as described above.
   */
  private @Nullable String readLineFromBuffer() {
    // try to find a newline (CR or LF) in the buffer
    // and construct a string from that single line
    var newlineIndex = this.findNewlineIndex();
    if (newlineIndex != -1) {
      return this.bufferToString(newlineIndex, true);
    }

    // in case the buffer is getting too large or this reader is closed
    // with content remaining, just return a string containing the chars
    // that are remaining in the buffer to clear it
    var bufferTooLarge = this.stringBuffer.length() > MAX_STRING_BUFFER_SIZE;
    var bufferedButClosed = this.reader == null && !this.stringBuffer.isEmpty();
    if (bufferTooLarge || bufferedButClosed) {
      return this.bufferToString(this.stringBuffer.length(), false);
    }

    return null;
  }

  /**
   * Converts the buffered chars into a string beginning at the first char in the buffer up until the given end index.
   * If {@code endLf} is true the CR, LF or CRLF is stripped from the buffer, else the last char is included in the
   * returned string as well. The returned chars are removed from the string buffer.
   *
   * @param endIndex the index to which the chars should be included in the returned string.
   * @param endLf    if the current buffer ends with an LF or CR.
   * @return a string constructed from the first char to the given end index of the string buffer.
   */
  private @NonNull String bufferToString(int endIndex, boolean endLf) {
    if (endLf) {
      var prevIndex = endIndex - 1;
      var prevIsCr = this.stringBuffer.length() >= prevIndex && this.stringBuffer.charAt(prevIndex) == '\r';
      var substringEnd = prevIsCr ? prevIndex : endIndex;
      var string = this.stringBuffer.substring(0, substringEnd);
      this.stringBuffer.delete(0, endIndex + 1);
      return string;
    } else {
      var string = this.stringBuffer.substring(0, endIndex);
      this.stringBuffer.delete(0, endIndex);
      return string;
    }
  }

  /**
   * Finds the position of a newline character in the string buffer. This method returns the index of a single LF or CR
   * or the position of the LF in a CRLF case.
   *
   * @return the index of the first newline, or -1 if no newline char was found.
   */
  private int findNewlineIndex() {
    var bufferLength = this.stringBuffer.length();
    for (var index = 0; index < bufferLength; index++) {
      var c = this.stringBuffer.charAt(index);
      if (c == '\r') {
        // return the position of the LF in a CRLF setup
        var nextCharIndex = index + 1;
        var nextCharIsLf = bufferLength > nextCharIndex && this.stringBuffer.charAt(nextCharIndex) == '\n';
        return nextCharIsLf ? nextCharIndex : index;
      }

      if (c == '\n') {
        return index;
      }
    }

    return -1;
  }

  /**
   * Returns true if the next call to {@link #readLine()} will read something from the underlying buffer or if this
   * reader is closed but some content is still available to be read.
   *
   * @return true if some content can be read immediately.
   * @throws IOException if an I/O error occurs.
   */
  public boolean ready() throws IOException {
    var reader = this.reader;
    return (reader != null && reader.ready()) || !this.stringBuffer.isEmpty();
  }

  /**
   * Closes the underlying reader.
   *
   * @throws IOException if an I/O error occurs.
   */
  @Override
  public void close() throws IOException {
    this.reader.close();
    this.reader = null;
  }
}
