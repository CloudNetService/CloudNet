package de.dytanic.cloudnet.driver.util;

import java.util.function.Function;

public class PrefixedMessageMapper<T> {

  private final String prefix;
  private final Function<T, String> messageMapper;
  private final String suffix;

  public PrefixedMessageMapper(Function<T, String> messageMapper) {
    this(null, messageMapper);
  }

  public PrefixedMessageMapper(String prefix, Function<T, String> messageMapper) {
    this(prefix, messageMapper, null);
  }

  public PrefixedMessageMapper(String prefix, Function<T, String> messageMapper, String suffix) {
    this.prefix = prefix;
    this.messageMapper = messageMapper;
    this.suffix = suffix;
  }

  public String getPrefix() {
    return this.prefix;
  }

  public Function<T, String> getMessageMapper() {
    return this.messageMapper;
  }

  public String getSuffix() {
    return this.suffix;
  }
}
