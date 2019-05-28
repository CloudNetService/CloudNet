package de.dytanic.cloudnet.driver.module;

import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class DefaultModuleTaskEntry implements IModuleTaskEntry {

  private IModuleWrapper moduleWrapper;

  private ModuleTask taskInfo;

  private Method handler;

  @Override
  public IModule getModule() {
    return this.moduleWrapper.getModule();
  }
}