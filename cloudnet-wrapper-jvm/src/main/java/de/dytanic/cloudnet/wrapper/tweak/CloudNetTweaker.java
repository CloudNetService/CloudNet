package de.dytanic.cloudnet.wrapper.tweak;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

/**
 * Tweaker for the Minecraft LaunchWrapper which prevents doubled loading of CloudNet classes
 */
public class CloudNetTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, final File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader launchClassLoader) {
        launchClassLoader.addClassLoaderExclusion("de.dytanic.cloudnet.wrapper.");
        launchClassLoader.addClassLoaderExclusion("de.dytanic.cloudnet.common.");
        launchClassLoader.addClassLoaderExclusion("de.dytanic.cloudnet.driver.");
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

}
