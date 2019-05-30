package de.dytanic.cloudnet.driver.module;

public enum ModuleLifeCycle {

  //Calls if the Module instance is now created,
  LOADED,
  //Calls when the Module should start
  STARTED,
  //Calls when the Module should stop
  STOPPED,
  //Calls when the Module want be unload
  UNLOADED,
  //If the classLoader is finalized
  UNUSEABLE;

}