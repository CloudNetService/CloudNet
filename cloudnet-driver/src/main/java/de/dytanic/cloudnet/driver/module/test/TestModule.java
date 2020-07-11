package de.dytanic.cloudnet.driver.module.test;

import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.defaults.DefaultModule;

public final class TestModule extends DefaultModule {

    @ModuleTask(event = ModuleLifeCycle.LOADED)
    public void runLoad() {
        System.setProperty("module_test_state", "loaded");
    }

    @ModuleTask(event = ModuleLifeCycle.STARTED)
    public void runStart() {
        System.setProperty("module_test_state", "started");
        try {
            Class.forName("joptsimple.OptionSet");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @ModuleTask(event = ModuleLifeCycle.STOPPED)
    public void runStop() {
        System.setProperty("module_test_state", "stopped");
    }

    @ModuleTask(event = ModuleLifeCycle.UNLOADED)
    public void runUnload() {
        System.setProperty("module_test_state", "unloaded");
    }

}
