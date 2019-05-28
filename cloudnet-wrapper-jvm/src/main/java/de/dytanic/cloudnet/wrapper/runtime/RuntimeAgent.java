package de.dytanic.cloudnet.wrapper.runtime;

import java.lang.instrument.Instrumentation;
import lombok.Getter;

/**
 * The RuntimeAgent provides the setup for all actions after the main method of
 * this wrapper
 */
public final class RuntimeAgent {

  /**
   * The instance of the Instrumentation which is from the premain() method
   * added
   *
   * @see Instrumentation
   */
  @Getter
  private static Instrumentation instrumentation;

  /**
   * The premain() for the java agent
   */
  public static void premain(String string, Instrumentation inst) {
    instrumentation = inst;
  }
}