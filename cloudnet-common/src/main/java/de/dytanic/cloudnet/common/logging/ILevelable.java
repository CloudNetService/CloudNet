package de.dytanic.cloudnet.common.logging;

/**
 * Allows an object to has an specific LogLevel as integer value
 *
 * @see LogLevel
 */
interface ILevelable {

  /**
   * Returns the current configured access level. All log entries under this level can be only noticed
   */
  int getLevel();

}
