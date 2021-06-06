package de.dytanic.cloudnet.driver.network.exception;

import io.netty.handler.codec.DecoderException;

public class SilentDecoderException extends DecoderException {

  public SilentDecoderException(String message) {
    super(message);
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
