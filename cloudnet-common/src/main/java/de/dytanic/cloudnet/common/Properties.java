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

package de.dytanic.cloudnet.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Properties extends LinkedHashMap<String, String> {

  /**
   * Parsed all properties, from a commandLine and split up into key/value pairs
   * <p>
   * "count_of_fingers --are fingers=16" will be to
   * <p>
   * count_of_fingers = true are = true fingers = 16
   *
   * @param line the following command line which should parsed
   * @return the current Properties object instance
   */
  public static Properties parseLine(String line) {
    if (line.trim().isEmpty()) {
      return null;
    }

    return parseLine(line.split(" "));
  }

  /**
   * Parsed all properties, from a commandLine and split up into key/value pairs
   * <p>
   * "count_of_fingers --are fingers=16" will be to
   * <p>
   * count_of_fingers = true are = true fingers = 16
   *
   * @param args the split command line arguments
   * @return the current Properties object instance
   */
  public static Properties parseLine(String[] args) {
    Properties properties = new Properties();

    for (String argument : args) {
      if (argument.isEmpty() || argument.equals(" ")) {
        continue;
      }

      if (argument.contains("=")) {
        int x = argument.indexOf("=");
        properties.put(argument.substring(0, x).replaceFirst("-", "").replaceFirst("-", ""), argument.substring(x + 1));
        continue;
      }

      if (argument.contains("--") || argument.contains("-")) {
        properties.put(argument.replaceFirst("-", "").replaceFirst("-", ""), "true");
        continue;
      }

      properties.put(argument, "true");
    }

    return properties;
  }

  /**
   * Returns a property parsed to String
   *
   * @param key the property, which should get
   * @return the property parsed as boolean
   */
  public boolean getBoolean(String key) {
    if (!this.containsKey(key)) {
      return false;
    }

    return Boolean.parseBoolean(this.get(key));
  }


  /**
   * The the data from a .properties file in the properties from by Sun/Oracle It do nothing, if the file is null or
   * doesn't exists
   *
   * @param file the path, from that the properties should load
   * @throws IOException if exists an error to read the file
   * @see java.util.Properties
   */
  public void load(File file) throws IOException {
    if (file != null) {
      this.load(file.toPath());
    }
  }

  /**
   * The the data from a .properties file in the properties from by Sun/Oracle It do nothing, if the file is null or
   * doesn't exists
   *
   * @param path the path, from that the properties should load
   * @throws IOException if exists an error to read the file
   * @see java.util.Properties
   */
  public void load(Path path) throws IOException {
    if (path != null && Files.exists(path)) {
      try (InputStream stream = Files.newInputStream(path)) {
        this.load(stream);
      }
    }
  }

  /**
   * The the data from a .properties file in the properties from by Sun/Oracle It do nothing, if the file is null or
   * doesn't exists
   *
   * @param inputStream the inputStream, from that the properties should load
   * @throws IOException if exists an error to read the file
   * @see java.util.Properties
   */
  public void load(InputStream inputStream) throws IOException {
    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      this.load(inputStreamReader);
    }
  }

  /**
   * The the data from a .properties file in the properties from by Sun/Oracle It do nothing, if the file is null or
   * doesn't exists
   *
   * @param reader the Reader, from that the properties should load
   * @throws IOException if exists an error to read the file
   * @see java.util.Properties
   */
  public void load(Reader reader) throws IOException {
    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      String input;

      while ((input = bufferedReader.readLine()) != null) {
        if (input.isEmpty() || input.equals(" ") || input.startsWith("#") || !input.contains("=")) {
          continue;
        }

        int x = input.indexOf("=");

        this.put(input.substring(0, x), input.substring(x + 1));
      }
    }
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param file the target file, which should be written
   * @throws IOException if an problem to write into the file exists
   * @see java.util.Properties
   */
  public void save(File file) throws IOException {
    if (file == null) {
      return;
    }

    this.save(null, file.toPath());
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param commit an optional comment on the file header
   * @param file   the target file, which should be written
   * @throws IOException if an problem to write into the file exists
   * @see java.util.Properties
   */
  public void save(String commit, File file) throws IOException {
    if (file == null) {
      return;
    }

    this.save(commit, file.toPath());
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param path the target file, which should be written
   * @throws IOException if an problem to write into the file exists
   * @see java.util.Properties
   */
  public void save(Path path) throws IOException {
    this.save(null, path);
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param commit an optional comment on the file header
   * @param path   the target file, which should be written
   * @throws IOException if an problem to write into the file exists
   * @see java.util.Properties
   */
  public void save(String commit, Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createFile(path);
    }

    try (OutputStream outputStream = Files.newOutputStream(path)) {
      this.save(commit, outputStream);
    }
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param outputStream the target outputStream, which should be written
   * @throws IOException if an problem to write into the file exists
   * @see java.util.Properties
   */
  public void save(OutputStream outputStream) throws IOException {
    this.save(null, outputStream);
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param commit       an optional comment on the file header
   * @param outputStream the target outputStream, which should be written
   * @throws IOException if an problem to write into the file exists
   * @see java.util.Properties
   */
  public void save(String commit, OutputStream outputStream) throws IOException {
    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      this.save(commit, outputStreamWriter);
    }
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param writer the target Writer, which should use
   * @see java.util.Properties
   */
  public void save(Writer writer) {
    this.save(null, writer);
  }

  /**
   * Writes all properties into the file like the .properties default in UTF-8
   *
   * @param commit an optional comment on the file header
   * @param writer the target Writer, which should use
   * @see java.util.Properties
   */
  public void save(String commit, Writer writer) {
    try (PrintWriter printWriter = new PrintWriter(writer)) {
      if (commit != null) {
        for (String key : commit.split("\n")) {
          printWriter.write("# " + key.replace("\n", "") + System.lineSeparator());
        }
      }

      for (Map.Entry<String, String> keys : this.entrySet()) {
        if (keys.getKey() != null && keys.getValue() != null) {
          printWriter.write(keys.getKey() + "=" + keys.getValue() + System.lineSeparator());
        }
      }

      printWriter.flush();
    }
  }

  /**
   * Map all entries on this instance to a new java.util.Properties object
   */
  public java.util.Properties java() {
    java.util.Properties properties = new java.util.Properties();
    properties.putAll(this);

    return properties;
  }
}
