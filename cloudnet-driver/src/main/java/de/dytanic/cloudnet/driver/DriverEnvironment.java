package de.dytanic.cloudnet.driver;

/**
 * Defines the environment, which the CloudNetDriver is implement
 *
 * @see CloudNetDriver
 */
public enum DriverEnvironment {

  /**
   * The driver implementation is on the node like the CloudNet class in node
   */
  CLOUDNET,

  /**
   * The driver implementation is on an application wrapper which runs on the JVM
   */
  WRAPPER,

  /**
   * The driver is implement in the driver module of this project
   */
  EMBEDDED
}
